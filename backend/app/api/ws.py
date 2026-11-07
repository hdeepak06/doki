import json
import logging
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from app.services.websocket_manager import ws_manager

logger = logging.getLogger("doki.ws_route")
router = APIRouter(tags=["WebSocket"])

@router.websocket("/ws/doki")
async def websocket_endpoint(websocket: WebSocket, client_type: str = "frontend", device_id: str = "ui"):
    if client_type == "device":
        await ws_manager.register_device(
            device_id=device_id,
            device_type=websocket.query_params.get("device_type", "unknown"),
            name=websocket.query_params.get("name", "Connected Device"),
            websocket=websocket,
            ip=websocket.client.host if websocket.client else None
        )
        try:
            while True:
                data = await websocket.receive_text()
                # Process incoming messages from device agent (e.g. command execution results)
                try:
                    payload = json.loads(data)
                    logger.info(f"Received message from device {device_id}: {payload.get('action')}")
                    # Broadcast telemetry to frontend
                    await ws_manager.broadcast_to_frontend({
                        "type": "device_telemetry",
                        "device_id": device_id,
                        "data": payload
                    })
                except Exception:
                    pass
        except WebSocketDisconnect:
            await ws_manager.unregister_device(device_id)
    else:
        # Frontend UI client
        await ws_manager.connect_client(websocket)
        try:
            while True:
                data = await websocket.receive_text()
                # Heartbeat or client action
                try:
                    payload = json.loads(data)
                    if payload.get("type") == "ping":
                        await websocket.send_text(json.dumps({"type": "pong"}))
                except Exception:
                    pass
        except WebSocketDisconnect:
            ws_manager.disconnect_client(websocket)

