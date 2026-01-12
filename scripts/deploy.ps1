# Deployment script for Minecraft plugins
# This script deploys compiled plugins to the server

param(
    [string]$PluginPath = ".",
    [string]$ServerPath = "C:\minecraft-server\plugins"
)

Write-Host "Deploying plugins to: $ServerPath"

# Build plugins with Maven
Write-Host "Building plugins..."
mvn clean package

# Copy compiled JARs to server
Write-Host "Copying JARs to server..."
Copy-Item "$PluginPath\*\target\*.jar" -Destination $ServerPath -Force

Write-Host "Deployment complete!"
