import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { authApi } from "../api/authApi";
import { setAuthToken, setUnauthorizedHandler } from "../api/httpClient";
import { profileStorage } from "../storage/profileStorage";
import { tokenStorage } from "../storage/tokenStorage";
import type { AuthCredentials, RegisterPayload } from "../types/auth";

type AuthContextValue = {
  isAuthenticated: boolean;
  isAuthLoading: boolean;
  profileName: string | null;
  signIn: (payload: AuthCredentials) => Promise<void>;
  signUp: (payload: RegisterPayload) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isAuthLoading, setIsAuthLoading] = useState(true);
  const [profileName, setProfileName] = useState<string | null>(null);

  const clearSession = useCallback(async () => {
    await Promise.all([tokenStorage.clearToken(), profileStorage.clearProfileName()]);
    setAuthToken(null);
    setProfileName(null);
    setIsAuthenticated(false);
  }, []);

  useEffect(() => {
    let isMounted = true;

    const bootstrapAuth = async () => {
      try {
        const [token, savedName] = await Promise.all([
          tokenStorage.getToken(),
          profileStorage.getProfileName()
        ]);
        if (isMounted) {
          setIsAuthenticated(Boolean(token));
          setAuthToken(token);
          setProfileName(savedName);
        }
      } finally {
        if (isMounted) {
          setIsAuthLoading(false);
        }
      }
    };

    void bootstrapAuth();

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => clearSession());
    return () => {
      setUnauthorizedHandler(null);
    };
  }, [clearSession]);

  const value = useMemo(
    () => ({
      isAuthenticated,
      isAuthLoading,
      profileName,
      signIn: async (payload: AuthCredentials) => {
        const { token } = await authApi.login(payload);
        await tokenStorage.setToken(token);
        setAuthToken(token);
        setIsAuthenticated(true);
      },
      signUp: async (payload: RegisterPayload) => {
        const { token } = await authApi.register(payload);
        await tokenStorage.setToken(token);
        setAuthToken(token);
        const nextName = payload.name?.trim() || null;
        if (nextName) {
          await profileStorage.setProfileName(nextName);
        }
        setProfileName(nextName);
        setIsAuthenticated(true);
      },
      signOut: async () => {
        await clearSession();
      }
    }),
    [clearSession, isAuthenticated, isAuthLoading, profileName]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
};
