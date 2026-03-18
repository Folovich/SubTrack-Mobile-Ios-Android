import { useDispatch, useSelector } from "react-redux";
import { authService } from "../services/authService";
import type { AppDispatch, RootState } from "../store";
import { clearAuth, setCredentials } from "../store/slices/authSlice";
import type { LoginRequest, RegisterRequest } from "../types/auth";

export const useAuth = () => {
  const dispatch = useDispatch<AppDispatch>();
  const auth = useSelector((state: RootState) => state.auth);

  const login = async (payload: LoginRequest) => {
    const session = await authService.login(payload);
    dispatch(setCredentials(session));
    return session;
  };

  const register = async (payload: RegisterRequest) => {
    const session = await authService.register(payload);
    dispatch(setCredentials(session));
    return session;
  };

  const logout = () => {
    authService.logout();
    dispatch(clearAuth());
  };

  return {
    ...auth,
    isAuthenticated: Boolean(auth.token),
    login,
    register,
    logout
  };
};
