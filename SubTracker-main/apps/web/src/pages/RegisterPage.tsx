import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { useLanguage } from "../i18n/LanguageProvider";

const RegisterPage = () => {
  const navigate = useNavigate();
  const { register } = useAuth();
  const { t } = useLanguage();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (password.length < 6) {
      setError("Пароль должен содержать минимум 6 символов");
      return;
    }

    if (password !== confirmPassword) {
      setError("Пароли не совпадают");
      return;
    }

    setError("");
    setIsSubmitting(true);

    try {
      await register({ email, password });
      navigate("/dashboard", { replace: true });
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Не удалось создать аккаунт");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-page__header">
        <p className="auth-page__eyebrow">{t("auth_register_eyebrow")}</p>
        <h1 className="auth-page__title">{t("auth_register_title")}</h1>
        <p className="auth-page__subtitle">{t("auth_register_subtitle")}</p>
      </div>

      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="auth-form__field">
          <span>{t("auth_email")}</span>
          <input
            className="input"
            type="email"
            placeholder="you@example.com"
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
            placeholder="Минимум 6 символов"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="new-password"
            required
          />
        </label>
        <label className="auth-form__field">
          <span>{t("auth_confirm_password")}</span>
          <input
            className="input"
            type="password"
            placeholder="Повторите пароль"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            autoComplete="new-password"
            required
          />
        </label>
        {error ? <p className="form-message form-message--error">{error}</p> : null}
        <button type="submit" className="button" disabled={isSubmitting}>
          {isSubmitting ? t("auth_register_loading") : t("auth_register_button")}
        </button>
      </form>

      <div className="auth-page__footer">
        <span>{t("auth_register_has_account")}</span>
        <Link to="/login" className="auth-page__link">
          {t("auth_register_login")}
        </Link>
      </div>
    </div>
  );
};

export default RegisterPage;
