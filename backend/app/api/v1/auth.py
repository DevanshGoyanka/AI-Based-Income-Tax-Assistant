"""Auth endpoints - login, register."""
from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, EmailStr
from sqlalchemy import select
from app.deps import DatabaseSession
from app.infra.db.models.user import User
from app.infra.security.jwt import create_access_token, hash_password, verify_password

router = APIRouter()


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str
    full_name: str


class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict


@router.post("/auth/login", response_model=AuthResponse)
async def login(req: LoginRequest, db: DatabaseSession):
    result = await db.execute(select(User).where(User.email == req.email))
    user = result.scalar_one_or_none()
    if not user or not verify_password(req.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="User inactive")
    
    token = create_access_token(str(user.id), {"role": user.role})
    return AuthResponse(
        access_token=token,
        user={"id": str(user.id), "email": user.email, "full_name": user.full_name, "role": user.role},
    )


@router.post("/auth/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
async def register(req: RegisterRequest, db: DatabaseSession):
    result = await db.execute(select(User).where(User.email == req.email))
    if result.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email already registered")
    
    user = User(
        email=req.email,
        password_hash=hash_password(req.password),
        full_name=req.full_name,
        role="staff",
        is_active=True,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    
    token = create_access_token(str(user.id), {"role": user.role})
    return AuthResponse(
        access_token=token,
        user={"id": str(user.id), "email": user.email, "full_name": user.full_name, "role": user.role},
    )
