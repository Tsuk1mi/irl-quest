use crate::error::AppError;
use regex::Regex;
use lazy_static::lazy_static;

lazy_static! {
    static ref EMAIL_REGEX: Regex = Regex::new(
        r"^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    ).unwrap();
}

pub fn validate_email(email: &str) -> Result<(), AppError> {
    if !EMAIL_REGEX.is_match(email) {
        return Err(AppError::Validation("Invalid email format".to_string()));
    }
    Ok(())
}

pub fn validate_password(password: &str) -> Result<(), AppError> {
    if password.len() < 8 {
        return Err(AppError::Validation(
            "Password must be at least 8 characters long".to_string(),
        ));
    }
    if !password.chars().any(|c| c.is_ascii_digit()) {
        return Err(AppError::Validation(
            "Password must contain at least one number".to_string(),
        ));
    }
    if !password.chars().any(|c| c.is_ascii_alphabetic()) {
        return Err(AppError::Validation(
            "Password must contain at least one letter".to_string(),
        ));
    }
    Ok(())
}

pub fn validate_username(username: &str) -> Result<(), AppError> {
    if username.len() < 3 || username.len() > 30 {
        return Err(AppError::Validation(
            "Username must be between 3 and 30 characters".to_string(),
        ));
    }
    if !username.chars().all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '-') {
        return Err(AppError::Validation(
            "Username can only contain letters, numbers, underscores and hyphens".to_string(),
        ));
    }
    Ok(())
}

pub fn validate_quest_title(title: &str) -> Result<(), AppError> {
    if title.is_empty() || title.len() > 200 {
        return Err(AppError::Validation(
            "Quest title must be between 1 and 200 characters".to_string(),
        ));
    }
    Ok(())
}

pub fn validate_difficulty(difficulty: i32) -> Result<(), AppError> {
    if !(1..=5).contains(&difficulty) {
        return Err(AppError::Validation(
            "Difficulty must be between 1 and 5".to_string(),
        ));
    }
    Ok(())
}

pub fn validate_priority(priority: &str) -> Result<(), AppError> {
    match priority {
        "low" | "medium" | "high" | "urgent" => Ok(()),
        _ => Err(AppError::Validation(
            "Priority must be one of: low, medium, high, urgent".to_string(),
        )),
    }
}
