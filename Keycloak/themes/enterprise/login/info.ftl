<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
            ${kcSanitize(msg("${messageHeader}"))?no_esc}
        <#else>
            ${kcSanitize(message.summary)?no_esc}
        </#if>
    <#elseif section = "form">
        <div id="kc-info-message">
            <p class="instruction">${kcSanitize(message.summary)?no_esc}<#if requiredActions??><#list requiredActions>: <b><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#items></b></#list></#if></p>

            <#assign loginTargetUrl = "http://localhost:4200/login">
            <#if pageRedirectUri?has_content>
                <#assign loginTargetUrl = pageRedirectUri>
            <#elseif (client.baseUrl)?has_content>
                <#assign loginTargetUrl = client.baseUrl>
            <#elseif url.loginUrl?has_content>
                <#assign loginTargetUrl = url.loginUrl>
            <#elseif url.loginRestartFlowUrl?has_content>
                <#assign loginTargetUrl = url.loginRestartFlowUrl>
            </#if>

            <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}" style="margin-top: 24px;">
                <a id="kc-login-button" href="${loginTargetUrl}" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} btn-primary">
                    ${msg("doLogIn")}
                </a>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
