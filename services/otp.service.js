const https = require('https');

function sendSMS(phoneNumber, otp) {
    const apiKey = process.env.PING4SMS_API_KEY || 'e1fc54301c0da93b95d3502a3151f420';
    const sender = process.env.PING4SMS_SENDER_ID || 'ASTELV';
    const route = process.env.PING4SMS_ROUTE || '4';
    const templateId = process.env.PING4SMS_TEMPLATE_ID || '1677100000000387344';

    const cleanPhone = phoneNumber.replace(/\D/g, '');
    // Ping4SMS domestic route needs the 10 digit number
    const mobile = cleanPhone.slice(-10);

    const message = `Dear Customer,Your OTP for login on ASTRO ELEVEN is ${otp}.Do not share it with any One. https://astroeleven.com/  `;

    const params = new URLSearchParams({
        key: apiKey,
        route: route,
        sender: sender,
        number: mobile,
        sms: message,
        templateid: templateId
    });

    const url = `https://site.ping4sms.com/api/smsapi?${params.toString()}`;

    https.get(url, (res) => {
        let data = '';
        res.on('data', (chunk) => data += chunk);
        res.on('end', () => console.log('PING4SMS Result:', data));
    }).on('error', (e) => {
        console.error('PING4SMS Error:', e);
    });
}

module.exports = { 
    sendSMS,
    sendMsg91: sendSMS // backward compatibility alias
};
