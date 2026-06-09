import https from 'https';
https.get('https://cdn.islamic.network/quran/audio/64/ar.alafasy/1.mp3', res => {
  console.log(res.statusCode);
});
