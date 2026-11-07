import json
import logging
from typing import Dict, Set, Optional
from fastapi import WebSocket

logger = logging.getLogger("doki.ws")

class WebSocketManager:
    def __init__(self):
        # Frontend UI clients
        self.frontend_clients: Set[WebSocket] = set()
        # Connected devices: device_id -> WebSocket
        self.active_devices: Dict[str, WebSocket] = {}
        # Device metadata: device_id -> info dict
        self.device_info: Dict[str, dict] = {}

    async def connect_client(self, websocket: WebSocket):
        await websocket.accept()
        self.frontend_clients.add(websocket)
        logger.info(f"Frontend client connected. Total: {len(self.frontend_clients)}")
        # Send current device status
        await self.broadcast_device_status()

    def disconnect_client(self, websocket: WebSocket):
        if websocket in self.frontend_clients:
            self.frontend_clients.remove(websocket)
            logger.info("Frontend client disconnected")

    async def register_device(self, device_id: str, device_type: str, name: str, websocket: WebSocket, ip: Optional[str] = None):
        await websocket.accept()
        self.active_devices[device_id] = websocket
        self.device_info[device_id] = {
            "device_id": device_id,
            "device_type": device_type,
            "name": name,
            "status": "online",
            "ip_address": ip or "",
            "battery_level": "100%"
        }
        logger.info(f"Device registered: {name} ({device_id}, {device_type})")
        await self.broadcast_device_status()

    async def unregister_device(self, device_id: str):
        if device_id in self.active_devices:
            del self.active_devices[device_id]
        if device_id in self.device_info:
            self.device_info[device_id]["status"] = "offline"
        logger.info(f"Device disconnected: {device_id}")
        await self.broadcast_device_status()

    async def broadcast_device_status(self):
        payload = {
            "type": "devices_update",
            "devices": list(self.device_info.values())
        }
        await self.broadcast_to_frontend(payload)

    async def broadcast_to_frontend(self, message: dict):
        text = json.dumps(message)
        dead_connections = set()
        for client in self.frontend_clients:
            try:
                await client.send_text(text)
            except Exception:
                dead_connections.add(client)
        for dead in dead_connections:
            self.frontend_clients.discard(dead)

    async def send_to_device(self, device_type: str, command: dict) -> bool:
        """Finds device by type ('laptop' or 'android') and transmits command."""
        target_ws = None
        for dev_id, info in self.device_info.items():
            if info.get("device_type") == device_type and info.get("status") == "online":
                target_ws = self.active_devices.get(dev_id)
                break
        
        if target_ws:
            try:
                await target_ws.send_text(json.dumps(command))
                return True
            except Exception as e:
                logger.error(f"Failed to transmit to {device_type}: {e}")
                return False
        return False

ws_manager = WebSocketManager()

