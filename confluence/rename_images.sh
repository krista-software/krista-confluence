#!/bin/bash

# Navigate to the _media directory
cd "$(dirname "$0")/src/main/resources/documentation/_media" || exit 1

# Rename images following the <MD_FILE_NAME>_<IMAGE_NAME>.png convention
mv api_key_generation.png CreatingConfluenceApp_api_key_generation.png
mv api_keys_management.png CreatingConfluenceApp_api_keys_management.png
mv manage_account.png CreatingConfluenceApp_manage_account.png
mv security_section.png CreatingConfluenceApp_security_section.png
mv confluence_extension.png ExtensionConfiguration_extension_catalog.png
mv confluence_extension_add_to_workspace.png ExtensionConfiguration_add_to_workspace.png
mv confluence_invoker_extension.png ExtensionConfiguration_extension_list.png
mv set_up_attributes.png ExtensionConfiguration_set_attributes.png
mv validate_attributes.png ExtensionConfiguration_validate_attributes.png

echo "Images renamed successfully!"
ls -la

