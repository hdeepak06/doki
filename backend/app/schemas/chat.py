from typing import List, Optional
from datetime import datetime
from pydantic import BaseModel, Field

class MessageBase(BaseModel):
    role: str
    content: str

class MessageCreate(MessageBase):
    pass

class MessageResponse(MessageBase):
    id: str
    conversation_id: str
    model: Optional[str] = None
    tokens_used: int = 0
    created_at: datetime

    class Config:
        from_attributes = True

class ConversationResponse(BaseModel):
    id: str
    title: str
    created_at: datetime
    updated_at: datetime
    messages: List[MessageResponse] = []

    class Config:
        from_attributes = True

class ChatRequest(BaseModel):
    conversation_id: Optional[str] = None
    message: str = Field(..., min_length=1)
    model: Optional[str] = None
    stream: bool = True
    target_device: Optional[str] = None  # 'laptop' or 'android' or 'backend'
    api_key: Optional[str] = None

