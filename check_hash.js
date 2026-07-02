const bcrypt = require('bcrypt');
const hash = "$2y$10$.GuPhIU0kebcZjCIH1Q4IOMY.kCSPYZBgN/knmQTUVgzijdB1eAQq".replace('$2y$', '$2a$'); // sometimes $2y$ is unsupported in node bcrypt depending on version, fallback $2a$ if needed
const words = ["password", "123456", "12345678", "admin", "admin123", "Riseastro", "astroeleven", "Riseastro123", "Riseastro@123", "astroeleven@123", "7777777777", "9876543210"];

const testHash = async () => {
    for(let w of words) {
        if(bcrypt.compareSync(w, "$2y$10$.GuPhIU0kebcZjCIH1Q4IOMY.kCSPYZBgN/knmQTUVgzijdB1eAQq")) {
            console.log("Found password:", w);
            return;
        }
        if(bcrypt.compareSync(w, hash)) {
            console.log("Found password:", w);
            return;
        }
    }
    console.log("Not found in common list.");
}
testHash();
