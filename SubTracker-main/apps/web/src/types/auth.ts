export interface User {
  id: number;
  email: string;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface AuthState {
  token: string | null;
  user: User | null;
}

export interface ApiErrorResponse {
  message?: string;
  errors?: Record<string, string>;
}
