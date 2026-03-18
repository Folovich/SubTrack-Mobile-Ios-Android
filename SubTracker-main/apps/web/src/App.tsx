import { Route, Routes } from "react-router-dom";
import AuthLayout from "./layouts/AuthLayout";
import ProtectedRoute from "./components/common/ProtectedRoute";
import MainLayout from "./layouts/MainLayout";
import AnalyticsPage from "./pages/AnalyticsPage";
import DashboardPage from "./pages/DashboardPage";
import ImportCsvPage from "./pages/ImportCsvPage";
import LandingPage from "./pages/LandingPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import RemindersPage from "./pages/RemindersPage";
import SettingsPage from "./pages/SettingsPage";
import SubscriptionsPage from "./pages/SubscriptionsPage";
import UpcomingPage from "./pages/UpcomingPage";

const App = () => {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route
        element={
          <ProtectedRoute>
            <MainLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/subscriptions" element={<SubscriptionsPage />} />
        <Route path="/upcoming" element={<UpcomingPage />} />
        <Route path="/reminders" element={<RemindersPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/import" element={<ImportCsvPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  );
};

export default App;
