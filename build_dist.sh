#!/bin/bash

# Configuration
VERSION="1.0.0"
PLUGIN_ID="com.jonbackhaus.metacrawler"
PLUGIN_UID="79832" # arbitrary but needs to be unique
JAR_NAME="magicdraw-metacrawler-$VERSION.jar"
DIST_DIR="dist"
DIST_DATE=$(date +%Y-%m-%d)
TEMP_DIR="dist/temp"
ZIP_NAME="metacrawler-plugin-v$VERSION.zip"
MDR_NAME="MDR_Plugin_Metacrawler_v${VERSION}_descriptor.xml"

echo "Building Metacrawler Distribution Bundle v$VERSION..."

# 1. Clean and build project
mvn clean package
if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit 1
fi

# 2. Setup folder structure
mkdir -p "$DIST_DIR"
rm -rf "$DIST_DIR"/*

mkdir -p "$TEMP_DIR/data/resourcemanager"
mkdir -p "$TEMP_DIR/plugins/$PLUGIN_ID"

# 3. Copy JAR file
cp "target/$JAR_NAME" "$TEMP_DIR/plugins/$PLUGIN_ID/"

# 4. Generate Plugin Descriptor
cat <<EOF > "$TEMP_DIR/plugins/$PLUGIN_ID/plugin.xml"
<plugin
        id="$PLUGIN_ID"
        name="Metacrawler Plugin"
        version="$VERSION"
        provider-name="Jonathan Backhaus"
        class="com.jonbackhaus.metacrawler.MetacrawlerPlugin">
    <runtime>
        <library name="$JAR_NAME"/>
    </runtime>
    <requires>
        <api version="1.0"/>
    </requires>
</plugin>
EOF

# 5. Generate Resource Manager Descriptor
cat <<EOF > "$TEMP_DIR/data/resourcemanager/$MDR_NAME"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<resourceDescriptor critical="false" date="$DIST_DATE" description="Recursive context menu for interactive metachain crawling in MagicDraw." id="$PLUGIN_UID" name="Metacrawler Plugin" mdVersionMax="higher" mxVersionMin="17.0" restartMagicdraw="true" type="Plugin">
    <version human="$VERSION" internal="1" resource="10" />
    <provider name="Jonathan Backhaus" homePage="https://github.com/jonbackhaus/magicdraw-metacrawler" />
    <edition>Reader</edition>
    <edition>Community</edition>
    <edition>Standard</edition>
    <edition>Professional Java</edition>
    <edition>Professional C++</edition>
    <edition>Professional</edition>
    <edition>Architect</edition>
    <edition>Enterprise</edition>
    <installation>
        <file from="plugins/$PLUGIN_ID/*.*" to="plugins/$PLUGIN_ID/*.*" />
        <file from="data/resourcemanager/$MDR_NAME" to="data/resourcemanager/$MDR_NAME" />
    </installation>
</resourceDescriptor>
EOF

# 6. Create Zip Bundle
cd "$TEMP_DIR"
zip -r "../../$DIST_DIR/$ZIP_NAME" . > /dev/null
cd ../..

# 7. Cleanup
rm -rf "$TEMP_DIR"

echo "Distribution bundle created: $DIST_DIR/$ZIP_NAME"
echo "Contents of $DIST_DIR/:"
ls -F "$DIST_DIR"
