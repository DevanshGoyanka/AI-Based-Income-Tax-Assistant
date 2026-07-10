"""Client management endpoints."""
from datetime import date
from uuid import UUID
from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from app.deps import CurrentUser, DatabaseSession
from app.infra.db.models.client import Client

router = APIRouter()


class ClientCreate(BaseModel):
    pan: str
    name: str
    mobile: str | None = None
    email: str | None = None
    dob: date | None = None


class ClientUpdate(BaseModel):
    """Fields that can be updated via PATCH."""
    name: str | None = None
    mobile: str | None = None
    email: str | None = None
    dob: date | None = None


class ClientResponse(BaseModel):
    id: str
    pan: str
    name: str
    mobile: str | None
    email: str | None
    category: str
    dob: date | None = None

    @classmethod
    def from_orm_obj(cls, c) -> "ClientResponse":
        return cls(
            id=str(c.id),
            pan=c.pan,
            name=c.name,
            mobile=c.mobile,
            email=c.email,
            category=c.category,
            dob=c.dob,
        )


@router.get("/clients", response_model=list[ClientResponse])
async def list_clients(db: DatabaseSession, current_user: CurrentUser):
    result = await db.execute(select(Client).order_by(Client.name))
    clients = result.scalars().all()
    return [ClientResponse.from_orm_obj(c) for c in clients]


@router.post("/clients", response_model=ClientResponse, status_code=status.HTTP_201_CREATED)
async def create_client(req: ClientCreate, db: DatabaseSession, current_user: CurrentUser):
    """Create a new client. Optionally set DOB at creation time."""
    client = Client(
        pan=req.pan.upper(),
        name=req.name,
        mobile=req.mobile,
        email=req.email,
        dob=req.dob,
        category="individual",
        created_by=current_user.id,
    )
    db.add(client)
    await db.commit()
    await db.refresh(client)
    return ClientResponse.from_orm_obj(client)


@router.patch("/clients/{client_id}", response_model=ClientResponse)
async def update_client(
    client_id: UUID,
    req: ClientUpdate,
    db: DatabaseSession,
    current_user: CurrentUser,
):
    """Update a client's fields. Use this to set DOB for ZIP decryption."""
    result = await db.execute(select(Client).where(Client.id == client_id))
    client = result.scalar_one_or_none()
    if not client:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Client not found")

    # Apply only provided (non-None) fields
    update_data = req.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(client, field, value)

    await db.commit()
    await db.refresh(client)
    return ClientResponse.from_orm_obj(client)


@router.get("/clients/{client_id}", response_model=ClientResponse)
async def get_client(client_id: UUID, db: DatabaseSession, current_user: CurrentUser):
    result = await db.execute(select(Client).where(Client.id == client_id))
    client = result.scalar_one_or_none()
    if not client:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Client not found")
    return ClientResponse.from_orm_obj(client)
