# Prerequisites
1. Create a [developer certificate](https://developer.apple.com/help/account/certificates/create-developer-id-certificates/)
   - Type: Developer ID Application
     - For DMG
   - Type: Developer ID Installer
     - For PKG
2. Export the certificate as a .p12 file from Keychain Access.
3. Create a [provisioning profile](https://developer.apple.com/account/resources/profiles/list)

# Local Signing
1. Modify the signing run config to add the environment variable `DeveloperId=Developer ID Application: FirstName LastName (XYZ123455)`
   - Use 'Installer' if using PKG

# Github Signing
See [github documentation](https://docs.github.com/en/actions/how-tos/deploy/deploy-to-third-party-platforms/sign-xcode-applications#creating-secrets-for-your-certificate-and-provisioning-profile).

