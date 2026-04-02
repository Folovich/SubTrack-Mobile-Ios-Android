const fs = require('fs');
const pdf = require('pdf-parse');
const input = 'c:/Users/Muhammed/Desktop/Цифровой_вызов_Задание_отборочного_этапа_«Отборочное_задание_Фулстэк».pdf';
const out = 'c:/Users/Muhammed/Documents/SubTrack/_tmp_tz_extracted.txt';
const data = fs.readFileSync(input);
pdf(data).then(r => {
  fs.writeFileSync(out, r.text, 'utf8');
  console.log('chars', r.text.length);
}).catch(err => {
  console.error(err);
  process.exit(1);
});
