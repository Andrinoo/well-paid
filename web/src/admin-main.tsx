import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AdminApp } from "./admin/AdminApp";
import { LocaleProvider } from "./i18n/LocaleProvider";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <LocaleProvider>
      <AdminApp />
    </LocaleProvider>
  </StrictMode>,
);
