import React, { useEffect, useRef } from 'react';

interface JarvisCoreProps {
  state?: 'idle' | 'listening' | 'thinking' | 'speaking';
  size?: number;
  onClick?: () => void;
}

export const JarvisCore: React.FC<JarvisCoreProps> = ({
  state = 'idle',
  size = 280,
  onClick,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;
    let angle = 0;
    const particles: Array<{ x: number; y: number; speed: number; radius: number; alpha: number; angle: number; dist: number }> = [];

    // Initialize ambient particles
    for (let i = 0; i < 36; i++) {
      particles.push({
        x: 0,
        y: 0,
        speed: 0.005 + Math.random() * 0.015,
        radius: 1 + Math.random() * 2,
        alpha: 0.2 + Math.random() * 0.8,
        angle: Math.random() * Math.PI * 2,
        dist: 40 + Math.random() * 70,
      });
    }

    const render = () => {
      ctx.clearRect(0, 0, size, size);
      const cx = size / 2;
      const cy = size / 2;

      // Color scheme based on state
      let primaryColor = 'rgba(6, 182, 212, ';    // Cyan
      let secondaryColor = 'rgba(59, 130, 246, '; // Blue

      if (state === 'listening') {
        primaryColor = 'rgba(16, 185, 129, ';   // Emerald green
        secondaryColor = 'rgba(5, 150, 105, ';
      } else if (state === 'thinking') {
        primaryColor = 'rgba(168, 85, 247, ';   // Purple/Violet
        secondaryColor = 'rgba(139, 92, 246, ';
      } else if (state === 'speaking') {
        primaryColor = 'rgba(245, 158, 11, ';   // Amber/Orange
        secondaryColor = 'rgba(239, 68, 68, ';
      }

      angle += state === 'thinking' ? 0.04 : 0.015;

      // 1. Central Core Glow
      const grad = ctx.createRadialGradient(cx, cy, 10, cx, cy, size / 2);
      grad.addColorStop(0, primaryColor + '0.8)');
      grad.addColorStop(0.25, primaryColor + '0.3)');
      grad.addColorStop(0.65, secondaryColor + '0.1)');
      grad.addColorStop(1, 'transparent');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, size, size);

      // 2. Concentric Holographic Rings
      const rings = [
        { r: size * 0.38, dashes: [8, 12], width: 1.5, rot: angle * 0.5 },
        { r: size * 0.32, dashes: [24, 8, 4, 8], width: 2, rot: -angle * 0.8 },
        { r: size * 0.24, dashes: [4, 6], width: 1, rot: angle * 1.2 },
        { r: size * 0.16, dashes: [], width: 2.5, rot: 0 },
      ];

      rings.forEach((ring) => {
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(ring.rot);
        ctx.beginPath();
        ctx.arc(0, 0, ring.r, 0, Math.PI * 2);
        ctx.strokeStyle = primaryColor + '0.6)';
        ctx.lineWidth = ring.width;
        ctx.setLineDash(ring.dashes);
        ctx.stroke();
        ctx.restore();
      });

      // 3. Radar-style Tick Marks
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(angle * 0.3);
      for (let i = 0; i < 24; i++) {
        const rad = (i * Math.PI * 2) / 24;
        const x1 = Math.cos(rad) * (size * 0.42);
        const y1 = Math.sin(rad) * (size * 0.42);
        const x2 = Math.cos(rad) * (size * 0.45);
        const y2 = Math.sin(rad) * (size * 0.45);
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.strokeStyle = primaryColor + (i % 3 === 0 ? '0.9)' : '0.3)');
        ctx.lineWidth = i % 3 === 0 ? 2 : 1;
        ctx.stroke();
      }
      ctx.restore();

      // 4. Orbiting Energy Particles
      particles.forEach((p) => {
        p.angle += p.speed;
        const px = cx + Math.cos(p.angle) * p.dist;
        const py = cy + Math.sin(p.angle) * p.dist;
        ctx.beginPath();
        ctx.arc(px, py, p.radius, 0, Math.PI * 2);
        ctx.fillStyle = primaryColor + `${p.alpha})`;
        ctx.fill();
      });

      // 5. Pulsing Inner Node
      const innerPulse = Math.sin(angle * 4) * 4;
      ctx.beginPath();
      ctx.arc(cx, cy, size * 0.08 + innerPulse, 0, Math.PI * 2);
      ctx.fillStyle = primaryColor + '0.95)';
      ctx.shadowColor = primaryColor + '1)';
      ctx.shadowBlur = 20;
      ctx.fill();
      ctx.shadowBlur = 0;

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [state, size]);

  return (
    <div
      onClick={onClick}
      className="relative flex items-center justify-center cursor-pointer select-none group"
      style={{ width: size, height: size }}
    >
      <canvas
        ref={canvasRef}
        width={size}
        height={size}
        className="transition-transform duration-500 transform group-hover:scale-105"
      />
      <div className="absolute inset-0 rounded-full border border-cyan-500/20 pointer-events-none group-hover:border-cyan-400/50 transition-colors" />
    </div>
  );
};

