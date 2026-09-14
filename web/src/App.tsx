import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { RedirectIfAuth, RequireAuth } from "./guards";
import { ConfirmEmailPage } from "./pages/ConfirmEmail";
import { DashboardPage } from "./pages/Dashboard";
import { ExpensesPage } from "./pages/Expenses";
import { FamilyPage } from "./pages/Family";
import { GoalsPage } from "./pages/Goals";
import { IncomesPage } from "./pages/Incomes";
import { InvestmentsPage } from "./pages/Investments";
import { LandingPage } from "./pages/Landing";
import { LoginPage } from "./pages/Login";
import { RecoverPage } from "./pages/Recover";
import { RegisterPage } from "./pages/Register";
import { ReservePage } from "./pages/Reserve";
import { SettingsPage } from "./pages/Settings";
import { ShoppingPage } from "./pages/Shopping";
import { AppShell } from "./shell";

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
          <Route element={<AppShell />}>
            <Route path="/app" element={<DashboardPage />} />
            <Route path="/app/despesas" element={<ExpensesPage />} />
            <Route path="/app/receitas" element={<IncomesPage />} />
            <Route path="/app/metas" element={<GoalsPage />} />
            <Route path="/app/investimentos" element={<InvestmentsPage />} />
            <Route path="/app/reserva" element={<ReservePage />} />
            <Route path="/app/listas" element={<ShoppingPage />} />
            <Route path="/app/familia" element={<FamilyPage />} />
            <Route path="/app/definicoes" element={<SettingsPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
