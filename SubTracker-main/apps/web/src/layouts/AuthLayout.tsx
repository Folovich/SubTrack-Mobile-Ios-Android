import { Link, Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

const AuthLayout = () => {
  const { isAuthenticated } = useAuth();

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="auth-shell">
      <div className="auth-shell__backdrop" />
      <div className="auth-card auth-card--single">
        <div className="auth-card__panel">
          <Link to="/" className="auth-card__brand landing-brand" aria-label="SubTrack">
            <span className="landing-brand__text">
              <span className="landing-brand__main">Sub</span>
              <span className="landing-brand__script">Track</span>
            </span>
          </Link>
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
