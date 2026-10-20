<#import "template.ftl" as layout>
<#assign resetEmailSent = false>
<#if message?? && message.summary?? && message.summary == msg("emailSentMessage")>
    <#assign resetEmailSent = true>
</#if>
<@layout.registrationLayout bodyClass="enterprise-reset-password" displayMessage=!resetEmailSent && !messagesPerField.existsError('username'); section>
    <#if section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <#if resetEmailSent>
            <div class="enterprise-reset-success" role="status" aria-live="polite">
                <div class="enterprise-reset-success-icon" aria-hidden="true">
                    <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <circle cx="24" cy="24" r="21" stroke="currentColor" stroke-width="2.5"/>
                        <path d="m14 24 7 7 14-15" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </div>
                <h2>${msg("enterpriseResetSentTitle")}</h2>
                <p>${msg("enterpriseResetSentBody")}</p>
                <a class="enterprise-reset-back" href="${url.loginUrl}">
                    <i class="fa fa-arrow-left" aria-hidden="true"></i>
                    ${msg("backToLogin")}
                </a>
            </div>
        <#else>
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
            <div class="enterprise-reset-icon" aria-hidden="true">
                <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="7" y="12" width="34" height="25" rx="5" stroke="currentColor" stroke-width="2.5"/>
                    <path d="m9 16 15 11 15-11" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M34 7v8m-4-4h8" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
            </div>
            <p class="enterprise-reset-intro">${msg("enterpriseResetPasswordHint")}</p>

            <div class="${properties.kcFormGroupClass!}">
                <label for="username" class="${properties.kcLabelClass!}">${msg("usernameOrEmail")}</label>
                <input
                    tabindex="1"
                    id="username"
                    name="username"
                    type="text"
                    class="${properties.kcInputClass!}"
                    value="${(auth.attemptedUsername!'')}"
                    placeholder="${msg('enterpriseUsernamePlaceholder')}"
                    autocomplete="username"
                    autofocus
                    aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                />
                <#if messagesPerField.existsError('username')>
                    <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('username'))?no_esc}
                    </span>
                </#if>
            </div>

            <div id="kc-form-buttons" class="${properties.kcFormGroupClass!} enterprise-reset-actions">
                <input
                    tabindex="2"
                    class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                    type="submit"
                    value="${msg('enterpriseSendResetLink')}"
                />
                <a tabindex="3" class="enterprise-reset-back" href="${url.loginUrl}">
                    <i class="fa fa-arrow-left" aria-hidden="true"></i>
                    ${msg("backToLogin")}
                </a>
            </div>
        </form>
        </#if>
    </#if>
</@layout.registrationLayout>
