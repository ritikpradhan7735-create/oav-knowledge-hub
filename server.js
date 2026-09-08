const express = require('express');
const path = require('path');
const fs = require('fs');
const multer = require('multer');
const cloudinary = require('cloudinary').v2;

const app = express();
const PORT = process.env.PORT || 3000;
const DATA = path.join(__dirname, 'notes.json');
const UPLOADS = path.join(__dirname, 'uploads');
fs.mkdirSync(UPLOADS, { recursive: true });

cloudinary.config({ cloud_name: process.env.CLOUDINARY_CLOUD_NAME, api_key: process.env.CLOUDINARY_API_KEY, api_secret: process.env.CLOUDINARY_API_SECRET });
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 20 * 1024 * 1024 } });
app.use(express.json({ limit: '2mb' }));
app.use(express.static(path.join(__dirname, 'public')));
app.use('/uploads', express.static(UPLOADS));

const adminOk = b => !!(process.env.ADMIN_USER && process.env.ADMIN_PASS && b && (b.username === process.env.ADMIN_USER || b.user === process.env.ADMIN_USER) && (b.password === process.env.ADMIN_PASS || b.pass === process.env.ADMIN_PASS));
const cloudReady = () => !!(process.env.CLOUDINARY_CLOUD_NAME && process.env.CLOUDINARY_API_KEY && process.env.CLOUDINARY_API_SECRET);
const readLocal = () => { try { return JSON.parse(fs.readFileSync(DATA, 'utf8')); } catch { return []; } };
const writeLocal = n => fs.writeFileSync(DATA, JSON.stringify(n, null, 2));

app.get('/api/health', (_, res) => res.json({ status: 'ok', service: 'OAV Hub', cloudinary: cloudReady() }));
app.get('/api/notes', async (_, res) => {
  try {
    if (cloudReady()) {
      const r = await cloudinary.search.expression('resource_type:raw AND folder:oav_hub_pdf_notes').with_field('context').sort_by('created_at','desc').max_results(500).execute();
      return res.json({ success:true, notes:r.resources.map(f => ({ id:f.public_id, class:(f.context||{}).classNum||'IX', subject:(f.context||{}).subject||'General', title:(f.context||{}).title||f.filename, fileUrl:f.secure_url })) });
    }
  } catch (e) { console.error('Cloud notes:', e.message); }
  res.json({ success:true, notes:readLocal() });
});
app.post('/api/upload-note', upload.single('pdf'), async (req,res) => {
  if (!adminOk(req.body)) return res.status(401).json({success:false,message:'Invalid admin credentials. Set ADMIN_USER and ADMIN_PASS on Render.'});
  if (!req.file || !/pdf$/i.test(req.file.originalname)) return res.status(400).json({success:false,message:'Please select a PDF file.'});
  const note = { id:`note_${Date.now()}_${Math.random().toString(36).slice(2,7)}`, class:req.body.classNum||'IX', subject:(req.body.subject||'General').trim(), title:(req.body.title||req.file.originalname).trim() };
  try {
    if (cloudReady()) {
      const result = await new Promise((resolve,reject) => { const s=cloudinary.uploader.upload_stream({resource_type:'raw',folder:'oav_hub_pdf_notes',public_id:note.id,format:'pdf',context:{classNum:note.class,subject:note.subject,title:note.title}},(e,r)=>e?reject(e):resolve(r)); s.end(req.file.buffer); });
      note.id=result.public_id; note.fileUrl=result.secure_url;
    } else {
      const filename=`${note.id}.pdf`; fs.writeFileSync(path.join(UPLOADS,filename),req.file.buffer); note.fileUrl=`/uploads/${filename}`;
      const notes=readLocal(); notes.unshift(note); writeLocal(notes);
    }
    res.json({success:true,note});
  } catch(e) { console.error(e); res.status(500).json({success:false,message:'Upload failed. Check storage configuration.'}); }
});
app.delete('/api/delete-note/:id', async (req,res) => {
  if (!adminOk(req.body)) return res.status(401).json({success:false,message:'Invalid admin credentials.'});
  const id=req.params.id;
  try {
    if (cloudReady()) await cloudinary.uploader.destroy(`oav_hub_pdf_notes/${id.replace(/^.*\//,'')}`,{resource_type:'raw'}).catch(()=>{});
    const notes=readLocal(); const note=notes.find(n=>n.id===id);
    if(note?.fileUrl?.startsWith('/uploads/')) fs.unlinkSync(path.join(__dirname,note.fileUrl));
    writeLocal(notes.filter(n=>n.id!==id));
    res.json({success:true});
  } catch(e){res.status(500).json({success:false,message:'Delete failed.'});}
});
app.post('/api/chat', async (req,res) => {
  const prompt=(req.body.prompt||'').trim();
  if(!prompt) return res.status(400).json({success:false,reply:'Please type a question.'});
  const key=process.env.GEMINI_API_KEY;
  if(!key) return res.json({success:false,reply:'Study Buddy AI is not configured yet. Add GEMINI_API_KEY in Render environment variables.'});
  try {
    const model=process.env.GEMINI_MODEL || 'gemini-1.5-flash';
    const r=await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({contents:[{parts:[{text:`You are Study Buddy for Indian school students. Give accurate, concise, easy-to-understand educational help. Question: ${prompt}`}]}]})});
    const d=await r.json(); const reply=d.candidates?.[0]?.content?.parts?.[0]?.text;
    res.json({success:!!reply,reply:reply||d.error?.message||'No answer generated.'});
  } catch(e){res.status(500).json({success:false,reply:'Unable to reach Study Buddy right now.'});}
});
app.get('*', (_,res)=>res.sendFile(path.join(__dirname,'public','index.html')));
app.listen(PORT,()=>console.log(`OAV Hub running on ${PORT}`));
