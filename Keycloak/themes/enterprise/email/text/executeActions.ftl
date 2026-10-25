<#assign isEmailVerification = requiredActions?? && requiredActions?seq_contains("VERIFY_EMAIL")>
<#assign headingKey = isEmailVerification?then("emailVerifyHeading", "emailResetHeading")>
<#assign introKey = isEmailVerification?then("emailVerifyIntro", "emailResetIntro")>
<#assign buttonKey = isEmailVerification?then("emailVerifyButton", "emailResetButton")>
${msg(headingKey)}

${msg("emailResetGreeting")}

${msg(introKey)}

${msg(buttonKey)}: ${link}

${msg("emailResetExpiry", linkExpirationFormatter(linkExpiration))}
${msg("emailResetFallback")}

${msg("emailResetSecurity")}
${msg("emailResetAutomated")}
