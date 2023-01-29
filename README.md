# Axocraft Plugin
This repository tracks code for customized server-side functionality for the axocraft server. It uses both a whitelist and token gating to allow users to access the (java) server.

## License
All code in this repo is available under the MIT License unless specified otherwise. All code is available as-is. Axolittles takes no risk or responsibility for anyone using this code.

## Community 
Join our Discord: https://discord.gg/axolittlesnft

## Build Plugin Jar
This project uses Maven. I don't really know much about java programming but basically I use 
Intellij community edition and created a scratch maven project. Seems like most dependencies are automatically downloaded 
by opening the pom.xml file in intellij and clicking the maven sync button.
Once it's synced you can build the jar artifact by expanding the maven window on the right, expand axocraft>lifecycle and double-click "package".

## Running the plugin
After creating the .jar artifact, drop it into the plugin folder inside your minecraft server. 