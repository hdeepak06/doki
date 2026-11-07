from fastapi import APIRouter
import psutil
from app.services.websocket_manager import ws_manager
from pydantic import BaseModel
from typing import Optional

router = APIRouter(prefix="/api/devices", tags=["Devices"])

class DeviceCommandRequest(BaseModel):
    target_device: str  # 'laptop' or 'android'
    action: str
    params: Optional[dict] = None

@router.get("")
async def get_devices():
    # 1. Fetch live metrics from this Windows laptop
    cpu_percent = psutil.cpu_percent(interval=None)
    mem = psutil.virtual_memory()
    battery = psutil.sensors_battery()
    battery_pct = f"{battery.percent:.0f}%" if battery else "AC Power"

    # Always represent local Windows host
    laptop_device = {
        "device_id": "windows_host",
        "name": "Deepak's Laptop",
        "device_type": "laptop",
        "status": "online",
        "os": "Windows",
        "cpu_usage": f"{cpu_percent}%",
        "ram_usage": f"{mem.used / (1024**3):.1f} GB / {mem.total / (1024**3):.1f} GB ({mem.percent}%)",
        "battery": battery_pct,
        "is_charging": battery.power_plugged if battery else True
    }

    # Fetch connected WebSocket devices (e.g. Android phone)
    connected_list = [laptop_device]
    for dev_id, info in ws_manager.device_info.items():
        if info.get("device_type") != "laptop":
            connected_list.append(info)

    # If Android phone is not yet connected via WS, show it as paired/offline for discovery
    if not any(d.get("device_type") == "android" for d in connected_list):
        connected_list.append({
            "device_id": "android_phone",
            "name": "Deepak's OPPO Phone",
            "device_type": "android",
            "status": "offline",
            "os": "Android",
            "battery": "Unknown",
            "note": "Connect via DOKI Android Companion App"
        })

    return {
        "devices": connected_list,
        "total": len(connected_list)
    }

@router.post("/command")
async def execute_command(req: DeviceCommandRequest):
    success = await ws_manager.send_to_device(req.target_device, {
        "action": req.action,
        "params": req.params or {}
    })
    return {
        "status": "transmitted" if success else "queued_or_target_offline",
        "target": req.target_device,
        "action": req.action
    }

