<#macro page link expiration isEmailVerification=false>
<#assign subjectKey = isEmailVerification?then("emailVerifySubject", "emailResetSubject")>
<#assign preheaderKey = isEmailVerification?then("emailVerifyPreheader", "emailResetPreheader")>
<#assign eyebrowKey = isEmailVerification?then("emailVerifyEyebrow", "emailResetEyebrow")>
<#assign headingKey = isEmailVerification?then("emailVerifyHeading", "emailResetHeading")>
<#assign introKey = isEmailVerification?then("emailVerifyIntro", "emailResetIntro")>
<#assign buttonKey = isEmailVerification?then("emailVerifyButton", "emailResetButton")>
<!doctype html>
<html lang="${locale.language!'vi'}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="color-scheme" content="light">
  <title>${msg(subjectKey)?html}</title>
</head>
<body style="margin:0;padding:0;background-color:#f3f6fa;font-family:Arial,Helvetica,sans-serif;color:#172033;">
  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
    ${msg(preheaderKey)?html}
  </div>
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f3f6fa;">
    <tr>
      <td align="center" style="padding:36px 16px;">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;background-color:#ffffff;border:1px solid #e5eaf1;border-radius:2px;overflow:hidden;">
          <tr>
            <td style="padding:24px 36px;background-color:#102a43;">
              <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                <tr>
                  <td width="38" height="38" align="center" valign="middle" style="width:38px;height:38px;border-radius:2px;background-color:#16a6a1;color:#ffffff;font-size:19px;font-weight:bold;">E</td>
                  <td style="padding-left:12px;color:#ffffff;font-size:15px;font-weight:bold;letter-spacing:1.1px;">${msg("emailBrand")?html}</td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="padding:38px 36px 16px;">
              <p style="margin:0 0 12px;color:#11817e;font-size:12px;font-weight:bold;letter-spacing:1.2px;text-transform:uppercase;">${msg(eyebrowKey)?html}</p>
              <h1 style="margin:0;color:#102a43;font-size:28px;line-height:1.25;font-weight:700;">${msg(headingKey)?html}</h1>
            </td>
          </tr>
          <tr>
            <td style="padding:8px 36px 0;color:#46566b;font-size:16px;line-height:1.65;">
              <p style="margin:0 0 16px;">${msg("emailResetGreeting")?html}</p>
              <p style="margin:0 0 22px;">${msg(introKey)?html}</p>
            </td>
          </tr>
          <tr>
            <td align="center" style="padding:4px 36px 24px;">
              <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                <tr>
                  <td align="center" style="border-radius:2px;background-color:#087f8c;">
                    <a href="${link?html}" style="display:inline-block;padding:14px 26px;border:1px solid #087f8c;border-radius:2px;color:#ffffff;font-size:15px;font-weight:bold;text-decoration:none;">${msg(buttonKey)?html}</a>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="padding:0 36px 8px;color:#65758b;font-size:14px;line-height:1.6;">
              <p style="margin:0 0 12px;">${msg("emailResetExpiry", linkExpirationFormatter(expiration))?html}</p>
              <p style="margin:0 0 18px;">${msg("emailResetFallback")?html}</p>
            </td>
          </tr>
          <tr>
            <td style="padding:20px 36px;background-color:#f8fafc;border-top:1px solid #e8edf3;color:#718096;font-size:12px;line-height:1.6;">
              <p style="margin:0 0 8px;">${msg("emailResetSecurity")?html}</p>
              <p style="margin:0;">${msg("emailResetAutomated")?html}</p>
            </td>
          </tr>
        </table>
        <p style="margin:18px 0 0;color:#8a97a8;font-size:11px;line-height:1.5;text-align:center;">${msg("emailResetCopyright")?html}</p>
      </td>
    </tr>
  </table>
</body>
</html>
</#macro>
