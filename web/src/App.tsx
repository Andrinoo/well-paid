import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { RedirectIfAuth, RequireAuth } from "./guards";
import { ConfirmEmailPage } from "./pages/ConfirmEmail";
import { DashboardPage } from "./pages/Dashboard";
import { LandingPage } from "./pages/Landing";
import { LoginPage } from "./pages/Login";
import { RecoverPage } from "./pages/Recover";
import { RegisterPage } from "./pages/Register";

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route
          path="/login"
          element={
            <RedirectIfAuth>
              <LoginPage />
            </RedirectIfAuth>
          }
        />
        <Route
          path="/registar"
          element={
            <RedirectIfAuth>
              <RegisterPage />
            </RedirectIfAuth>
          }
        />
        <Route path="/confirmar-email" element={<ConfirmEmailPage />} />
        <Route path="/recuperar" element={<RecoverPage />} />
        <Route element={<RequireAuth />}>
          <Route path="/app" element={<DashboardPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
