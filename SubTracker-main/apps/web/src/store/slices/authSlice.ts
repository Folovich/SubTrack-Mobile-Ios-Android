import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { authService } from "../../services/authService";
import type { AuthResponse, AuthState } from "../../types/auth";

const session = authService.getSession();

const initialState: AuthState = {
  token: session?.token ?? null,
  user: session?.user ?? null
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<AuthResponse>) => {
      state.token = action.payload.token;
      state.user = action.payload.user;
    },
    clearAuth: (state) => {
      state.token = null;
      state.user = null;
    }
  }
});

export const { clearAuth, setCredentials } = authSlice.actions;

export default authSlice.reducer;
