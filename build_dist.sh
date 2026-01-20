#!/bin/bash

# Configuration
VERSION="1.0"
PLUGIN_ID="com.jonbackhaus.metacrawler"
JAR_NAME="magicdraw-metacrawler-1.0-SNAPSHOT.jar"
DIST_DIR="dist"
TEMP_DIR="dist/temp"
ZIP_NAME="metacrawler-plugin-v$VERSION.zip"

echo "Building Metacrawler Distribution Bundle v$VERSION..."

# 1. Clean and build project
mvn clean package
if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit 1
fi

# 2. Setup folder structure
# Ensure dist directory exists and is empty except for what we are about to create
mkdir -p "$DIST_DIR"
rm -rf "$DIST_DIR"/*

mkdir -p "$TEMP_DIR/data/resourcemanager"
mkdir -p "$TEMP_DIR/plugins/$PLUGIN_ID"

# 3. Copy files
cp "target/$JAR_NAME" "$TEMP_DIR/plugins/$PLUGIN_ID/"
cp "src/main/resources/plugin.xml" "$TEMP_DIR/plugins/$PLUGIN_ID/"

# 4. Generate Resource Manager Descriptor
cat <<EOF > "$TEMP_DIR/data/resourcemanager/MDK_Metacrawler_v${VERSION}_resource.xml"
<?xml version="1.0" encoding="UTF-8"?>
<resourceDescriptor>
    <id>$PLUGIN_ID</id>
    <name>Metacrawler Plugin</name>
    <version>$VERSION</version>
    <provider-name>Jonathan Backhaus</provider-name>
    <description>Recursive context menu for interactive metachain crawling in MagicDraw.</description>
</resourceDescriptor>
EOF

# 5. Create Zip Bundle
cd "$TEMP_DIR"
zip -r "../../$DIST_DIR/$ZIP_NAME" . > /dev/null
cd ../..

# 6. Cleanup
rm -rf "$TEMP_DIR"

echo "Distribution bundle created: $DIST_DIR/$ZIP_NAME"
echo "Contents of $DIST_DIR/:"
ls -F "$DIST_DIR"
