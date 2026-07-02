// index.js - Simple file upload server using Express and Multer
const express = require('express');
const multer = require('multer');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Configure storage destination and filename
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    // Save uploads in 'uploads' directory relative to project root
    cb(null, path.join(__dirname, 'uploads'));
  },
  filename: (req, file, cb) => {
    // Preserve original filename
    cb(null, file.originalname);
  }
});

const upload = multer({ storage });

// Ensure uploads folder exists
const fs = require('fs');
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir);
}

// Serve a simple HTML form for testing
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'upload.html'));
});

// POST endpoint to receive file uploads
app.post('/upload', upload.single('file'), (req, res) => {
  if (!req.file) {
    return res.status(400).send('No file uploaded.');
  }
  res.send(`File \"${req.file.originalname}\" uploaded successfully.`);
});

app.listen(PORT, () => {
  console.log(`Node upload server listening on http://localhost:${PORT}`);
});
