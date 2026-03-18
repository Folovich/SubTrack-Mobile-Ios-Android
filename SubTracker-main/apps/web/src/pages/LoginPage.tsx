import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { useLanguage } from "../i18n/LanguageProvider";

const LoginPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const { t } = useLanguage();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const from =
    (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? "/dashboard";

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    try {
      await login({ email, password });
      navigate(from, { replace: true });
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Не удалось войти");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-page__header">
        <p className="auth-page__eyebrow">{t("auth_login_eyebrow")}</p>
        <h1 className="auth-page__title">{t("auth_login_title")}</h1>
        <p className="auth-page__subtitle">{t("auth_login_subtitle")}</p>
      </div>

      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="auth-form__field">
          <span>{t("auth_email")}</span>
          <input
            className="input"
            type="email"
            placeholder="demo@subtrack.app"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="email"
            required
          />
        </label>
        <label className="auth-form__field">
          <span>{t("auth_password")}</span>
          <input
            className="input"
            type="password"
            placeholder={t("auth_password")}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error ? <p className="form-message form-message--error">{error}</p> : null}
        <button type="submit" className="button" disabled={isSubmitting}>
          {isSubmitting ? t("auth_login_loading") : t("auth_login_button")}
        </button>
      </form>

      <div className="auth-page__footer">
        <span>{t("auth_login_no_account")}</span>
        <Link to="/register" className="auth-page__link">
          {t("auth_login_register")}
        </Link>
      </div>
    </div>
  );
};

export default LoginPage;
