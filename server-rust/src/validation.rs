use crate::error::AppError;
use regex::Regex;
use tracing::error;

fn compile_regex(pattern: &str) -> Result<Regex, AppError> {
    Regex::new(pattern).map_err(|e| {
        error!("Failed to compile regex '{}': {:?}", pattern, e);
        AppError::Internal("Server regex initialization error".to_string())
    })
}

// Note: compiling regex on each validation call is acceptable for this project; if
// performance becomes a concern, we can cache compiled regexes with a non-panicking
// once-cell that returns errors instead of panicking.

pub fn validate_email(email: &str) -> Result<(), AppError> {
    let re = compile_regex(r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")?;
    if !re.is_match(email) {
        return Err(AppError::BadRequest("Неверный формат email".to_string()));
    }
    Ok(())
}

pub fn validate_password(password: &str) -> Result<(), AppError> {
    // Ensure minimum length
    if password.len() < 8 {
        return Err(AppError::BadRequest(
            "Пароль должен содержать минимум 8 символов, включая буквы и цифры".to_string()
        ));
    }

    // Must contain at least one ASCII letter
    if !password.chars().any(|c| c.is_ascii_alphabetic()) {
        return Err(AppError::BadRequest(
            "Пароль должен содержать минимум одну букву".to_string()
        ));
    }

    // Must contain at least one ASCII digit
    if !password.chars().any(|c| c.is_ascii_digit()) {
        return Err(AppError::BadRequest(
            "Пароль должен содержать минимум одну цифру".to_string()
        ));
    }

    // Allowed characters: letters, digits and special chars @$!%*#?&
    let re = compile_regex(r"^[A-Za-z\d@$!%*#?&]+$")?;
    if !re.is_match(password) {
        return Err(AppError::BadRequest(
            "Пароль содержит недопустимые символы".to_string()
        ));
    }
    Ok(())
}

pub fn validate_username(username: &str) -> Result<(), AppError> {
    let re = compile_regex(r"^[a-zA-Z0-9_-]{3,20}$")?;
    if !re.is_match(username) {
        return Err(AppError::BadRequest(
            "Имя пользователя должно содержать от 3 до 20 символов и может включать буквы, цифры, подчеркивания и дефисы".to_string()
        ));
    }
    Ok(())
}

pub fn validate_task_title(title: &str) -> Result<(), AppError> {
    if title.trim().is_empty() {
        return Err(AppError::BadRequest("Название задачи не может быть пустым".to_string()));
    }
    if title.len() > 200 {
        return Err(AppError::BadRequest(
            "Название задачи не может быть длиннее 200 символов".to_string()
        ));
    }
    Ok(())
}

pub fn validate_quest_title(title: &str) -> Result<(), AppError> {
    if title.trim().is_empty() {
        return Err(AppError::BadRequest("Название квеста не может быть пустым".to_string()));
    }
    if title.len() > 200 {
        return Err(AppError::BadRequest(
            "Название квеста не может быть длиннее 200 символов".to_string()
        ));
    }
    Ok(())
}

pub fn validate_status(status: &str) -> Result<(), AppError> {
    match status {
        "pending" | "in_progress" | "completed" | "cancelled" | "failed" => Ok(()),
        _ => Err(AppError::BadRequest(
            "Недопустимый статус. Допустимые значения: pending, in_progress, completed, cancelled, failed".to_string()
        )),
    }
}

pub fn validate_priority(priority: &str) -> Result<(), AppError> {
    match priority {
        "low" | "medium" | "high" | "urgent" => Ok(()),
        _ => Err(AppError::BadRequest(
            "Недопустимый приоритет. Допустимые значения: low, medium, high, urgent".to_string()
        )),
    }
}
