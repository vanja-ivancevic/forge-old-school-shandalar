// ~*~ Wizard Wand Sparkle Trail ~*~
(function(){
  var sparkles=[];
  var colors=['#FFFF00','#FFD700','#FF8C00','#FF00FF','#00FFFF','#FFFFFF','#FFB6C1'];
  var shapes=['*','+','.','\u2726','\u2727','\u2728','\u2735'];
  document.addEventListener('mousemove',function(e){
    var s=document.createElement('span');
    s.textContent=shapes[Math.floor(Math.random()*shapes.length)];
    s.style.cssText='position:fixed;pointer-events:none;z-index:9999;font-size:'+(8+Math.random()*14)+'px;color:'+colors[Math.floor(Math.random()*colors.length)]+';left:'+e.clientX+'px;top:'+e.clientY+'px;';
    document.body.appendChild(s);
    sparkles.push({el:s,x:e.clientX,y:e.clientY,vx:(Math.random()-0.5)*2,vy:-1-Math.random()*2,life:1});
  });
  setInterval(function(){
    for(var i=sparkles.length-1;i>=0;i--){
      var p=sparkles[i];
      p.life-=0.03;
      p.x+=p.vx;
      p.y+=p.vy;
      p.vy+=0.05;
      p.el.style.left=p.x+'px';
      p.el.style.top=p.y+'px';
      p.el.style.opacity=p.life;
      if(p.life<=0){p.el.remove();sparkles.splice(i,1);}
    }
  },30);
})();
