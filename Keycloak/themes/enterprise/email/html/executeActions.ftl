<#import "reset-layout.ftl" as reset>
<#assign isEmailVerification = requiredActions?? && requiredActions?seq_contains("VERIFY_EMAIL")>
<@reset.page link=link expiration=linkExpiration isEmailVerification=isEmailVerification />
