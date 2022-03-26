#!/bin/bash

# Script for code-signing AIJ application package, storing it in
# a disk image, and notarizing it via Apple's servers.
# Eric Jensen, ejensen1@swarthmore.edu, 2022; License: GPL-3.0

# This script assumes that you have already stored an app-specific
# password for the 'altool' command in your keychain with name
# DEV_PASSWORD.  Steps to set that up:
# 1. See example of generating password at https://support.apple.com/en-us/HT204397
# 2. Once generated, put it in your keychain using this command:
#     xcrun altool --store-password-in-keychain-item "DEV_PASSWORD" -u <USERNAME> --password <PASSWORD>
#     where you substitute your own username (for Apple account) and app-specific password.

# The above step is one-time-only for part 1 (password is stored
# on-line in your Apple account), and one-time per machine you're
# using for signing for part 2 (password is stored locally in keychain).

# In addition, you need to have the 'create-dmg' script installed
# (https://github.com/create-dmg/create-dmg) and to place the
# background.png file

# Below we set some variables that other functions will use; edit as
# needed, but you shouldn't need to edit below this function.

set_config_vars() {
    # Set some variables used in the rest of the script. Note that
    # 'readonly' declares variables with global scope, even when used
    # inside a function.

    # Signing certificate:
    readonly CERT="Developer ID Application: Firstname Lastname (XYZ1234567)"

    # Username for Apple Developer account used for signing.  
    readonly DEV_ID="username@gmail.com" 

    # Application base name; include path here if in different
    # directory from where you're running the script: 
    readonly APPROOT="AstroImageJ"
    # Above, with '.app' appended; probably no need to change this.
    readonly APP="${APPROOT}.app"

    # Location and name of the .png file with the background used for
    # the disk image window: 
    BACKGROUND_PATH="/Users/username/Documents/aij_development/"
    readonly BACKGROUND="${BACKGROUND_PATH}background.png"

    # Check that the specified background image file exists:
    if [ ! -e $BACKGROUND ]
    then
	printf "\n\a!!! Disk image background file $BACKGROUND not found.\n"
	printf "\nCheck path, edit script, and try again.\n\n"
	exit 1
    # and that the AIJ image is there:
    elif [ ! -e $APP ]
    then
	printf "\n\a!!! Application file $APP not found.\n"
	printf "\nCheck path, edit script, and try again.\n\n"
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
}


# All the steps we need to take; individual functions defined below,
# then 'main' called at end of file. 
main() {
    set_config_vars
    make_entitlements_file
    sign_all_files
    sign_application
    verify_signing
    create_disk_image
    submit_image_for_notarization
    verify_notarization
    staple_notarization
    printf '\n*** Done! ***\n'
}

make_entitlements_file() {
    # Create a file with necessary entitlements for signing, per
    # https://stackoverflow.com/questions/58548736/notarize-existing-java-application-for-macos-catalina
    # Put the entitlements into a temporary file to pass to the
    # signing commands:
    readonly ENTITLEMENTS=`mktemp`
    cat <<- 'End_of_Entitlements' > $ENTITLEMENTS
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
    find $APP -type f -exec codesign -f --timestamp --sign="$CERT" \
	 --entitlements="$ENTITLEMENTS" --options=runtime {} \; 2>&1 \
	| grep -Ev "replacing existing signature"
}

sign_application() {
    # And the app directory itself:
    printf "*** Signing the application itself... ***\n\n" 
    codesign -f -v --timestamp --entitlements="$ENTITLEMENTS" --sign="$CERT" \
	     --options=runtime $APP
    # Clean up after ourselves by removing the entitlements file now
    # that we are done with it: 
    rm -f "$ENTITLEMENTS"
}

verify_signing() {
    # Verify the code signing:
    printf "\n*** Verifying signing... ***\n\n"
    spctl -a -t exec -vv $APP
    # Check the status of the last command to make sure we signed OK:
    status=$?
    if [ $status -ne 0 ]
    then
	>&2 printf "\n\n !!! Signing the application failed, exiting. !!! \n\n"
	exit $status
    fi
}

create_disk_image() {
    # Create archive using commands based on Apple's instructions at
    # https://developer.apple.com/documentation/xcode/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow

    # Create the .dmg file: 
    printf "\n\n*** Creating dmg file (Finder window will open and close)...***\n\n" 
    # Try to unmount existing disk image just in case:
    diskutil quiet unmount ${APPROOT} 2>&1 |grep -Ev "Unmount failed for ${APPROOT}"
    create-dmg --background $BACKGROUND --hdiutil-quiet \
	       --no-internet-enable --icon $APP 120 175 \
	       --text-size 16 --icon-size 128  \
	       --app-drop-link 420 175 --window-size 540 382 \
	       --volname ${APPROOT} ${APPROOT}.dmg ${APP}

    # Check the status of the last command to make sure we created the disk image OK:
    status=$?
    if [ $status -ne 0 ]
    then
	printf "\n\n !!! Creation of dmg file failed. !!! \n\n"
	printf "Try freeing up memory and running again.\n\n"
	exit $status
    fi
}

submit_image_for_notarization() {
    # Submit to Apple for notarizing: 
    printf "\n\n*** Submitting dmg file to Apple for notarizing... ***\n\n" 
    submit_status=`xcrun altool --notarize-app --primary-bundle-id com.$APPROOT.dmg \
	  -p "@keychain:DEV_PASSWORD" -u "$DEV_ID" --file $APPROOT.dmg`
    # Get the UUID of the submission, and set a global var we can use
    # to check status later: 
    readonly uuid=`echo "$submit_status" | awk '$1 == "RequestUUID" {print $3}' `
    # The above will submit, but the notarization takes a little while to complete:
    printf "*** Submitted for notarization; waiting 2 minutes to check status... ***\n\n"
    # Will take at least a few minutes, so sleep before continuing:
    sleep 120
}

verify_notarization() {
    notary_status=`xcrun altool -p "@keychain:DEV_PASSWORD" -u "$DEV_ID" \
    			 --notarization-info $uuid |grep Status:`
    while [[ $notary_status == *"in progress"* ]]
    do
	echo "Notarization in progress, sleeping 1 minute..."
	sleep 60
	notary_output=`xcrun altool -p "@keychain:DEV_PASSWORD" \
			     -u "$DEV_ID"  --notarization-info $uuid`
	notary_status=`echo "$notary_output" | grep Status:`
    done

    printf "\n\n Notarization done with status: \n"
    echo "$notary_output"
    readonly final_status=`echo "$notary_output" |grep "Status Message:"`
}

staple_notarization() {
    # Once the notarization completes, staple the resulting approval
    # to the disk image if successful: 
    if [[ $final_status == *"Status Message: Package Approved"* ]]
    then
	printf "\n\n*** Stapling notarization to package file... ***\n\n"
	xcrun stapler staple $APPROOT.dmg
    else
	printf "\n\n*** Unsuccessful notarization, not stapling. ***\n"
	printf "*** Final status of notarization: "
	echo "$final_status"
    fi
}

# Run the main function with any command-line arguments: 
main "$@"
