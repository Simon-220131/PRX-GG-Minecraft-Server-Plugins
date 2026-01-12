# Restart Minecraft server
# This script restarts the Minecraft server

param(
    [string]$ServerPath = "C:\minecraft-server"
)

Write-Host "Restarting Minecraft server..."

# Stop the server
Write-Host "Stopping server..."
# Add your stop command here (e.g., stop server via rcon or script)

# Wait for server to stop
Start-Sleep -Seconds 5

# Start the server
Write-Host "Starting server..."
# Add your start command here

Write-Host "Server restart complete!"
