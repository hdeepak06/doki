import React, { useEffect, useRef } from 'react';

interface AudioWaveformProps {
  active?: boolean;
  color?: string;
  height?: number;
}

export const AudioWaveform: React.FC<AudioWaveformProps> = ({
  active = false,
  color = '#06B6D4',
  height = 48,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let frameId: number;
    let step = 0;
    const barCount = 32;

    const render = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const w = canvas.width / barCount;

      for (let i = 0; i < barCount; i++) {
        const offset = (i / barCount) * Math.PI * 2;
        let barHeight = 4;

        if (active) {
          // Dynamic harmonic waveform
          barHeight =
            Math.sin(step + offset * 3) * 14 +
            Math.cos(step * 1.5 + offset * 2) * 8 +
            Math.sin(step * 0.8 + i) * 6 +
            18;
          barHeight = Math.max(4, Math.min(canvas.height - 4, barHeight));
        }

        const x = i * w + w * 0.2;
        const y = (canvas.height - barHeight) / 2;

        ctx.fillStyle = color;
        ctx.shadowColor = color;
        ctx.shadowBlur = active ? 8 : 2;
        ctx.beginPath();
        ctx.roundRect(x, y, w * 0.6, barHeight, 3);
        ctx.fill();
      }

      step += 0.15;
      frameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(frameId);
    };
  }, [active, color]);

  return (
    <canvas
      ref={canvasRef}
      width={240}
      height={height}
      className="w-full max-w-[240px] opacity-90 transition-opacity"
    />
  );
};

