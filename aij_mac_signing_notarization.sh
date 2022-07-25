#!/bin/bash

# Script for code-signing AIJ application package, storing it in
# a disk image, and notarizing it via Apple's servers.
# Eric Jensen, ejensen1@swarthmore.edu, 2022; License: GPL-3.0

# This script assumes that you have already stored an app-specific
# password for the 'altool' or 'notarytool' commands in your keychain
# with name AC_PASSWORD.  Steps to set that up:
# 1. See example of generating password at https://support.apple.com/en-us/HT204397
# 2. Once generated, put it in your keychain using this command:
#    xcrun notarytool store-credentials "AC_PASSWORD" --apple-id "myname@gmail.com" --team-id "XYZ12345"
#     where you substitute your own username (for Apple account) and the "team-id" is the
#     trailing string you see in "Developer ID Application" signing certificate, for example
#     in "Developer ID Application: Eric Jensen (XYZ123455)" it's the part in parentheses.
#   When you run that command, you will be prompted for the app-specific password
#    that you created in step 1. 

# The above step is one-time-only for part 1 (password is stored
# on-line in your Apple account), and one-time per machine you're
# using for signing for part 2 (password is stored locally in keychain).

# In addition, you need to have the 'create-dmg' script installed
# (https://github.com/create-dmg/create-dmg) and to place the
# background.png file

# Below we set some variables that other functions will use; edit as
# needed, but you shouldn't need to edit below this function.

# Uncomment this if you want to see each command as it is executed:
# set -x

set_config_vars() {
    # Set some variables used in the rest of the script. Note that
    # 'readonly' declares variables with global scope, even when used
    # inside a function.

    # Signing certificate:
    readonly CERT="Developer ID Application: Firstname Lastname (XYZ1234567)"

    # Path to where a copy of AstroImageJ.app is; it will be copied to
    # a temporary directory for signing and packaging, and the
    # original won't be modified.  
    readonly APP_PATH="/Users/username/Documents/aij_development/"

    # Path to write the signed and notarized .dmg file.  Here we
    # set to the same path as the input app, but change if needed:
    readonly DMG_PATH="$APP_PATH"
    
    # Location and name of the .png file with the background used for
    # the disk image window.  If in the same directory as the
    # app file, just set the path to APP_PATH. 
    BACKGROUND_PATH="$APP_PATH"
    readonly BACKGROUND="${BACKGROUND_PATH}/background.png"

    # Shouldn't need to change any of these.
    readonly APPROOT="AstroImageJ"
    # Above, with '.app' appended.  
    readonly APP="${APPROOT}.app"
    # Full path to the .dmg file:
    readonly DMG="${DMG_PATH}/${APPROOT}.dmg"
}

# All the steps we need to take; individual functions defined below,
# then 'main' called at end of file.
main() {
    set_config_vars
    check_files
    make_entitlements_file
    sign_all_files
    sign_application
    create_disk_image
    submit_image_for_notarization
    printf '\n*** Done! ***\n\n'
}

check_files() {
    # Check that some necessary input files exist, and that the
    # desired output file isn't there already. 

    # Check that the specified application file exists:
    if [ ! -e "${APP_PATH}/${APP}" ]
    then
        printf "\n\a!!! Application file ${APP_PATH}/${APP} not found.\n"
        printf "\nCheck path, edit script, and try again.\n\n"
        exit 1
    # Check that the specified background image file exists:
    elif [ ! -e "$BACKGROUND" ]
    then
        printf "\n\a!!! Disk image background file $BACKGROUND not found.\n"
        printf "\nCheck path, edit script, and try again.\n\n"
        exit 1
    fi

    # Make sure the target disk image file doesn't exist already:
    if [ -e "$DMG" ]
    then
        printf "\n\a!!! Output disk image file ${DMG} already exists.\n"
        printf "\nMove or rename, and re-run script.\n\n"
        exit 1
    fi
    
    # Also check that the 'create-dmg' script is installed:
    if ! command -v create-dmg &> /dev/null
    then
        printf "\n\a!!! create-dmg script is not installed. \n\n"
        printf "Install with 'brew install create-dmg' if homebrew is installed \n"
        printf "or from https://github.com/create-dmg/create-dmg \n\n"
        exit 1
    fi

    # Make sure we can create a temporary directory for our working
    # copy of the application, and copy it there:
    # Create a temporary directory that will hold the files for the disk image:
    readonly TEMP_DIR=`mktemp -d /tmp/AIJ_temp.XXXXX`
    cp -a "${APP_PATH}/${APP}" "$TEMP_DIR"
    
    # Check the status of the last command to make sure we copied the file OK:
    status=$?
    if [ $status -ne 0 ]
    then
        printf "\n\n !!! Failed to copy ${APP_PATH}/${APP} to temporary dir $TEMP_DIR !!! \n\n"
        exit $status
    fi
}

make_entitlements_file() {
    # Create a file with necessary entitlements for signing, per
    # https://stackoverflow.com/questions/58548736/notarize-existing-java-application-for-macos-catalina
    # Put the entitlements into a temporary file to pass to the
    # signing commands:
    readonly ENTITLEMENTS=`mktemp /tmp/AIJ_temp.XXXXX`
    cat <<- 'End_of_Entitlements' > "$ENTITLEMENTS"
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
	<key>com.apple.security.cs.allow-jit</key>
    	<true/>
    	<key>com.apple.security.cs.allow-unsigned-executable-memory</key>
    	<true/>
    	<key>com.apple.security.cs.disable-executable-page-protection</key>
    	<true/>
    	<key>com.apple.security.cs.disable-library-validation</key>
    	<true/>
    	<key>com.apple.security.cs.allow-dyld-environment-variables</key>
    	<true/>
    </dict>
    </plist>
End_of_Entitlements
}

sign_all_files() {  
    # Find each ordinary file within the bundle (not directories) and sign:
    printf "\n*** Signing all files... ***\n\n"
    # Grep out messages about replacing existing signature here since
    # they clutter things up and make it hard to see any real errors:
    find "${TEMP_DIR}/${APP}" -type f -exec codesign -f --timestamp --sign="$CERT" \
	 --entitlements="$ENTITLEMENTS" --options=runtime {} \; 2>&1 \
	| grep -Ev "replacing existing signature"
}

sign_application() {
    # And the app directory itself:
    printf "*** Signing the application itself... ***\n\n" 
    codesign -f -v --timestamp --entitlements="$ENTITLEMENTS" --sign="$CERT" \
	     --options=runtime "${TEMP_DIR}/${APP}"
    # Clean up after ourselves by removing the entitlements file now
    # that we are done with it: 
    rm -f "$ENTITLEMENTS"
}


create_disk_image() {
    # Create archive using commands based on Apple's instructions at
    # https://developer.apple.com/documentation/xcode/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow

    # Create the .dmg file: 
    printf "\n\n*** Creating dmg file (Finder window will open and close)...***\n\n"
    # Try to unmount existing disk image just in case:
    diskutil quiet unmount "${APPROOT}" 2>&1 |grep -Ev "Unmount failed for ${APPROOT}"
    create-dmg --background "$BACKGROUND" \
	       --no-internet-enable --icon "$APP" 120 175 \
           --hdiutil-quiet \
	       --text-size 16 --icon-size 128  \
	       --app-drop-link 420 175 --window-size 540 382 \
	       --volname "${APPROOT}" "$DMG" "${TEMP_DIR}/${APP}"

    # Check the status of the last command to make sure we created the disk image OK:
    status=$?
    if [ $status -ne 0 ] || [ ! -e "$DMG" ]
    then
        printf "\n\n !!! Creation of dmg file $DMG failed. !!! \n\n"
        printf "\n If no obvious error is shown above, "
        printf "try freeing up memory and running again.\n\n"
        exit $status
    else
        # Successfully created image, clean up temp dir:
        rm -rf "${TEMP_DIR}/${APP}"
        rmdir "${TEMP_DIR}"
    fi
}

submit_image_for_notarization() {
    # Submit to Apple for notarizing: 
    printf "\n\n*** Submitting dmg file to Apple for notarizing... ***\n\n"
    # Capture output to a variable as well as sending to screen:
    notary_status_text=$(xcrun notarytool submit "$DMG" --keychain-profile "AC_PASSWORD" --wait 2>&1 | tee /dev/tty )
    # Get the final status so we can decide what to do next:
    notary_status=$(echo "$notary_status_text" | perl -ne 'if (/^ *status: ([a-zA-Z]+)$/) {print $1}')
    if [[ $notary_status != "Accepted" ]]
    then
        printf "\n\n*** Unsuccessful notarization, not stapling. ***\n"
        logfile="${DMG_PATH}/notarization_log.json"
        printf "*** Fetching notarization log... ***\n"
        notary_request_id=$(echo "$notary_status_text" | perl -ne 'if (/^ *id: ([\w-]+)$/) {print $1;exit}')
        xcrun notarytool log "$notary_request_id" --keychain-profile "AC_PASSWORD" "$logfile"
        printf "*** See $logfile for details of notarization errors. ***\n"
        # Exit script with an error
        exit 1
    else
        printf "\n*** Stapling notarization to package file... ***"
        xcrun stapler staple "$DMG"
    fi
}

# Run the main function with any command-line arguments: 
main "$@"
