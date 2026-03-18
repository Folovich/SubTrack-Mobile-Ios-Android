export interface AuthCredentials {
  email: string;
  password: string;
}

export interface RegisterPayload extends AuthCredentials {
  name?: string;
}

export interface AuthUser {
  id: number;
  email: string;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  user?: AuthUser;
}
