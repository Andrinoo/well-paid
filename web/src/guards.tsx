import type { ReactNode } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { isSignedIn } from "./session";

export function RequireAuth() {
  if (!isSignedIn()) return <Navigate to="/login" replace />;
  return <Outlet />;
}

export function RedirectIfAuth({ children }: { children: ReactNode }) {
  if (isSignedIn()) return <Navigate to="/app" replace />;
  return children;
}
