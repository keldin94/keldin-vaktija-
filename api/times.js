module.exports = async function(req,res) {
  res.setHeader('Access-Control-Allow-Origin','*');
  res.setHeader('Cache-Control','public, s-maxage=900, stale-while-revalidate=3600');
  const c=Number(req.query.city), date=String(req.query.date||'');
  if(!Number.isInteger(c)||c<0||c>118||!/^20\d{2}-\d{2}-\d{2}$/.test(date))return res.status(400).json({error:'Neispravni parametri'});
  const [y,m,d]=date.split('-').map(Number);
  if(new Date(Date.UTC(y,m-1,d)).toISOString().slice(0,10)!==date)return res.status(400).json({error:'Neispravan datum'});
  const endpoint='https://api.vaktija.ba/vaktija/v1/'+c+'/'+y+'/'+m+'/'+d;
  try{
    const response=await fetch(endpoint,{headers:{Accept:'application/json'},signal:AbortSignal.timeout(9000)});
    if(!response.ok)throw new Error('Upstream '+response.status);
    const data=await response.json();
    if(!Array.isArray(data.vakat)||data.vakat.length!==6)throw Error('Pogrešan oblik API odgovora');
    return res.status(200).json({lokacija:data.lokacija,datum:data.datum,vakat:data.vakat});
  }catch(e){return res.status(502).json({error:'Izvor vaktije trenutno nije dostupan'});}
};