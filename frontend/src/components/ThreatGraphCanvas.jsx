import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ZoomIn,
  ZoomOut,
  RotateCcw,
  Search,
  Filter,
  ExternalLink,
  Copy,
  Check,
  X,
  ShieldAlert,
  Mail,
  Flame,
  Globe,
  Server,
  FileText,
  Link as LinkIcon,
  Layers,
  ChevronRight
} from 'lucide-react';

const NODE_COLORS = {
  PRIMARY_EMAIL: '#38bdf8',
  EMAIL: '#06b6d4',
  CAMPAIGN: '#eab308',
  URL: '#f59e0b',
  DOMAIN: '#f59e0b',
  IP: '#ef4444',
  ATTACHMENT: '#ec4899',
  ASN: '#8b5cf6',
  DEFAULT: '#94a3b8'
};

export default function ThreatGraphCanvas({ graphData, height = '560px', onNodeClick }) {
  const containerRef = useRef(null);
  const [nodes, setNodes] = useState([]);
  const [links, setLinks] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);
  const [filterType, setFilterType] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [copiedText, setCopiedText] = useState(false);

  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [isPanning, setIsPanning] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [draggedNode, setDraggedNode] = useState(null);

  const navigate = useNavigate();

  useEffect(() => {
    if (!graphData || !graphData.nodes || graphData.nodes.length === 0) {
      setNodes([]);
      setLinks([]);
      return;
    }

    const rawNodes = graphData.nodes;
    const rawLinks = graphData.links || [];

    const containerWidth = containerRef.current ? containerRef.current.clientWidth : 800;
    const containerHeight = 560;
    const centerX = containerWidth / 2;
    const centerY = containerHeight / 2;

    const primaryNode = rawNodes.find((n) => n.metadata?.isPrimary || n.type === 'CAMPAIGN');
    const positionedNodes = rawNodes.map((node, index) => {
      if (primaryNode && node.id === primaryNode.id) {
        return { ...node, x: centerX, y: centerY, vx: 0, vy: 0 };
      }

      const angle = (index / (rawNodes.length || 1)) * 2 * Math.PI;
      const radius = node.type === 'EMAIL' || node.type === 'CAMPAIGN' ? 160 : 230 + ((index % 3) * 35);
      const x = centerX + Math.cos(angle) * radius;
      const y = centerY + Math.sin(angle) * radius;

      return {
        ...node,
        x,
        y,
        vx: 0,
        vy: 0
      };
    });

    setNodes(positionedNodes);
    setLinks(rawLinks);
    setPan({ x: 0, y: 0 });
    setZoom(1);
    setSelectedNode(null);
  }, [graphData]);

  const filteredNodes = useMemo(() => {
    return nodes.filter((n) => {
      const matchesFilter =
        filterType === 'ALL' ||
        (filterType === 'EMAIL' && (n.type === 'EMAIL' || n.group === 'PRIMARY_EMAIL')) ||
        (filterType === 'CAMPAIGN' && n.type === 'CAMPAIGN') ||
        (filterType === 'URL' && (n.type === 'URL' || n.type === 'DOMAIN')) ||
        (filterType === 'IP' && (n.type === 'IP' || n.type === 'ASN')) ||
        (filterType === 'ATTACHMENT' && n.type === 'ATTACHMENT');

      const matchesSearch =
        !searchQuery.trim() ||
        n.label.toLowerCase().includes(searchQuery.toLowerCase()) ||
        n.type.toLowerCase().includes(searchQuery.toLowerCase());

      return matchesFilter && matchesSearch;
    });
  }, [nodes, filterType, searchQuery]);

  const visibleNodeIds = useMemo(() => {
    return new Set(filteredNodes.map((n) => n.id));
  }, [filteredNodes]);

  const visibleLinks = useMemo(() => {
    return links.filter((l) => visibleNodeIds.has(l.source) && visibleNodeIds.has(l.target));
  }, [links, visibleNodeIds]);

  const handleMouseDownContainer = (e) => {
    if (e.target.tagName === 'circle' || e.target.tagName === 'text' || e.target.closest('.inspector-panel') || e.target.closest('.control-panel')) {
      return;
    }
    setIsPanning(true);
    setDragStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
  };

  const handleMouseMove = (e) => {
    if (isPanning) {
      setPan({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y
      });
    } else if (draggedNode) {
      const rect = containerRef.current.getBoundingClientRect();
      const mouseX = (e.clientX - rect.left - pan.x) / zoom;
      const mouseY = (e.clientY - rect.top - pan.y) / zoom;

      setNodes((prev) =>
        prev.map((n) => (n.id === draggedNode ? { ...n, x: mouseX, y: mouseY } : n))
      );
    }
  };

  const handleMouseUp = () => {
    setIsPanning(false);
    setDraggedNode(null);
  };

  const handleWheel = (e) => {
    e.preventDefault();
    const zoomFactor = 1.1;
    if (e.deltaY < 0) {
      setZoom((prev) => Math.min(prev * zoomFactor, 3));
    } else {
      setZoom((prev) => Math.max(prev / zoomFactor, 0.4));
    }
  };

  const handleNodeMouseDown = (e, nodeId) => {
    e.stopPropagation();
    setDraggedNode(nodeId);
    const node = nodes.find((n) => n.id === nodeId);
    if (node) {
      setSelectedNode(node);
      if (onNodeClick) onNodeClick(node);
    }
  };

  const handleCopy = (text) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedText(true);
    setTimeout(() => setCopiedText(false), 2000);
  };

  const getNodeColor = (node) => {
    if (node.color) return node.color;
    if (node.metadata?.isPrimary) return NODE_COLORS.PRIMARY_EMAIL;
    return NODE_COLORS[node.type] || NODE_COLORS.DEFAULT;
  };

  const getNodeIcon = (type) => {
    switch (type) {
      case 'EMAIL':
        return <Mail className="w-3.5 h-3.5 text-white" />;
      case 'CAMPAIGN':
        return <Flame className="w-3.5 h-3.5 text-white" />;
      case 'URL':
        return <LinkIcon className="w-3.5 h-3.5 text-white" />;
      case 'DOMAIN':
        return <Globe className="w-3.5 h-3.5 text-white" />;
      case 'IP':
        return <Server className="w-3.5 h-3.5 text-white" />;
      case 'ATTACHMENT':
        return <FileText className="w-3.5 h-3.5 text-white" />;
      default:
        return <Layers className="w-3.5 h-3.5 text-white" />;
    }
  };

  const nodePositions = useMemo(() => {
    const map = new Map();
    nodes.forEach((n) => map.set(n.id, { x: n.x, y: n.y }));
    return map;
  }, [nodes]);

  return (
    <div
      ref={containerRef}
      className="relative w-full overflow-hidden rounded-xl bg-slate-950 border border-slate-800 select-none shadow-2xl"
      style={{ height }}
      onMouseDown={handleMouseDownContainer}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onWheel={handleWheel}
    >
      <div
        className="absolute inset-0 pointer-events-none opacity-20"
        style={{
          backgroundImage: 'radial-gradient(#38bdf8 1px, transparent 1px)',
          backgroundSize: '24px 24px',
          transform: `translate(${pan.x % 24}px, ${pan.y % 24}px)`
        }}
      />

      <div className="control-panel absolute top-3 left-3 z-10 flex flex-wrap items-center gap-2 bg-slate-900/90 backdrop-blur-md p-2 rounded-xl border border-slate-800 shadow-lg">
        <div className="relative">
          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
          <input
            id="graph-search-input"
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search nodes..."
            className="w-36 sm:w-44 pl-8 pr-3 py-1 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-200 focus:outline-none focus:border-cyan-500"
          />
        </div>

        <div className="flex items-center gap-1 border-l border-slate-800 pl-2">
          {['ALL', 'EMAIL', 'CAMPAIGN', 'URL', 'IP', 'ATTACHMENT'].map((type) => (
            <button
              key={type}
              id={`graph-filter-${type.toLowerCase()}`}
              onClick={() => setFilterType(type)}
              className={`px-2 py-1 rounded-md text-[11px] font-medium transition-colors ${
                filterType === type
                  ? 'bg-cyan-600 text-white shadow-sm shadow-cyan-500/20'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
              }`}
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      <div className="control-panel absolute top-3 right-3 z-10 flex items-center gap-1 bg-slate-900/90 backdrop-blur-md p-1.5 rounded-xl border border-slate-800 shadow-lg">
        <button
          id="graph-zoom-in-btn"
          onClick={() => setZoom((z) => Math.min(z * 1.2, 3))}
          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800 transition-colors"
          title="Zoom In"
        >
          <ZoomIn className="w-4 h-4" />
        </button>
        <button
          id="graph-zoom-out-btn"
          onClick={() => setZoom((z) => Math.max(z / 1.2, 0.4))}
          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800 transition-colors"
          title="Zoom Out"
        >
          <ZoomOut className="w-4 h-4" />
        </button>
        <button
          id="graph-reset-view-btn"
          onClick={() => {
            setPan({ x: 0, y: 0 });
            setZoom(1);
          }}
          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800 transition-colors"
          title="Reset View"
        >
          <RotateCcw className="w-4 h-4" />
        </button>
        <span className="text-[11px] text-slate-500 px-2 font-mono border-l border-slate-800">
          {Math.round(zoom * 100)}%
        </span>
      </div>

      <div className="absolute bottom-3 left-3 z-10 flex items-center gap-2 bg-slate-900/90 backdrop-blur-md px-3 py-1.5 rounded-lg border border-slate-800 text-[11px] text-slate-400">
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-cyan-400" />
          Nodes: <strong className="text-slate-200 font-mono">{filteredNodes.length}</strong>
        </span>
        <span className="text-slate-700">|</span>
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-slate-500" />
          Links: <strong className="text-slate-200 font-mono">{visibleLinks.length}</strong>
        </span>
      </div>

      {nodes.length === 0 ? (
        <div className="w-full h-full flex flex-col items-center justify-center text-slate-500 gap-2">
          <Layers className="w-8 h-8 text-slate-600 animate-pulse" />
          <p className="text-sm">No Graph Entities Available</p>
        </div>
      ) : (
        <svg
          className="w-full h-full cursor-grab active:cursor-grabbing"
          style={{ width: '100%', height: '100%' }}
        >
          <g transform={`translate(${pan.x}, ${pan.y}) scale(${zoom})`}>
            <defs>
              <marker
                id="graph-arrow"
                viewBox="0 0 10 10"
                refX="22"
                refY="5"
                markerWidth="6"
                markerHeight="6"
                orient="auto-start-reverse"
              >
                <path d="M 0 0 L 10 5 L 0 10 z" fill="#475569" />
              </marker>
            </defs>

            {visibleLinks.map((link, idx) => {
              const srcPos = nodePositions.get(link.source);
              const tgtPos = nodePositions.get(link.target);
              if (!srcPos || !tgtPos) return null;

              const isHighlighted =
                selectedNode &&
                (selectedNode.id === link.source || selectedNode.id === link.target);

              const midX = (srcPos.x + tgtPos.x) / 2;
              const midY = (srcPos.y + tgtPos.y) / 2;

              return (
                <g key={`link-${idx}`}>
                  <line
                    x1={srcPos.x}
                    y1={srcPos.y}
                    x2={tgtPos.x}
                    y2={tgtPos.y}
                    stroke={isHighlighted ? '#38bdf8' : '#334155'}
                    strokeWidth={isHighlighted ? 2.5 : 1.2}
                    strokeDasharray={link.type === 'HISTORICAL_MATCH' ? '4 3' : 'none'}
                    markerEnd="url(#graph-arrow)"
                    className="transition-colors"
                  />
                  {link.label && (
                    <text
                      x={midX}
                      y={midY - 4}
                      fill={isHighlighted ? '#94a3b8' : '#64748b'}
                      fontSize="9"
                      fontFamily="monospace"
                      textAnchor="middle"
                      className="pointer-events-none select-none"
                    >
                      {link.label}
                    </text>
                  )}
                </g>
              );
            })}

            {filteredNodes.map((node) => {
              const isSelected = selectedNode?.id === node.id;
              const nodeColor = getNodeColor(node);
              const radius = node.val ? Math.max(12, Math.min(node.val, 26)) : 14;

              return (
                <g
                  key={node.id}
                  id={`graph-node-${node.id}`}
                  transform={`translate(${node.x}, ${node.y})`}
                  onMouseDown={(e) => handleNodeMouseDown(e, node.id)}
                  className="cursor-pointer group"
                >
                  {isSelected && (
                    <circle
                      r={radius + 8}
                      fill="none"
                      stroke={nodeColor}
                      strokeWidth="2"
                      strokeDasharray="4 3"
                      className="animate-spin-slow opacity-80"
                    />
                  )}

                  <circle
                    r={radius + 4}
                    fill={nodeColor}
                    opacity="0.15"
                    className="group-hover:opacity-30 transition-opacity"
                  />

                  <circle
                    r={radius}
                    fill="#0f172a"
                    stroke={isSelected ? '#ffffff' : nodeColor}
                    strokeWidth={isSelected ? 3 : 2}
                    className="transition-all"
                  />

                  <foreignObject
                    x={-8}
                    y={-8}
                    width={16}
                    height={16}
                    className="pointer-events-none flex items-center justify-center"
                  >
                    <div className="flex items-center justify-center w-full h-full">
                      {getNodeIcon(node.type)}
                    </div>
                  </foreignObject>

                  {node.riskScore !== null && node.riskScore !== undefined && (
                    <g transform={`translate(${radius - 4}, ${-radius + 4})`}>
                      <circle r="7" fill="#ef4444" />
                      <text
                        fill="#ffffff"
                        fontSize="8"
                        fontWeight="bold"
                        textAnchor="middle"
                        dy="3"
                        className="pointer-events-none select-none"
                      >
                        {node.riskScore}
                      </text>
                    </g>
                  )}

                  <text
                    x={0}
                    y={radius + 14}
                    fill={isSelected ? '#38bdf8' : '#e2e8f0'}
                    fontSize="11"
                    fontWeight={isSelected ? '600' : '500'}
                    textAnchor="middle"
                    className="pointer-events-none select-none transition-colors"
                    style={{
                      textShadow: '0 1px 3px rgba(0,0,0,0.9), 0 0 4px rgba(15,23,42,0.9)'
                    }}
                  >
                    {node.label.length > 24 ? node.label.substring(0, 22) + '...' : node.label}
                  </text>
                </g>
              );
            })}
          </g>
        </svg>
      )}

      {selectedNode && (
        <div
          id="graph-inspector-panel"
          className="inspector-panel absolute bottom-4 right-4 z-20 w-80 max-w-[calc(100%-32px)] bg-slate-900/95 backdrop-blur-md border border-slate-700/80 rounded-2xl shadow-2xl p-4 text-xs transition-all animate-in fade-in slide-in-from-bottom-2"
        >
          <div className="flex items-start justify-between gap-2 pb-3 border-b border-slate-800">
            <div className="flex items-center gap-2 min-w-0">
              <span
                className="w-2.5 h-2.5 rounded-full shrink-0"
                style={{ backgroundColor: getNodeColor(selectedNode) }}
              />
              <span className="font-mono text-[10px] px-2 py-0.5 rounded uppercase font-semibold bg-slate-800 text-slate-300">
                {selectedNode.type}
              </span>
              {selectedNode.riskScore !== null && selectedNode.riskScore !== undefined && (
                <span className="text-[10px] px-1.5 py-0.5 rounded font-bold bg-rose-950 text-rose-300 border border-rose-800 font-mono">
                  Score {selectedNode.riskScore}
                </span>
              )}
            </div>

            <button
              onClick={() => setSelectedNode(null)}
              className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition-colors"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="py-3 space-y-2">
            <div>
              <span className="text-[10px] uppercase font-semibold text-slate-500">Label / Entity</span>
              <div className="flex items-center justify-between gap-2 mt-0.5">
                <p className="font-mono text-slate-200 text-xs break-all select-text font-medium">
                  {selectedNode.label}
                </p>
                <button
                  onClick={() => handleCopy(selectedNode.label)}
                  className="shrink-0 p-1 text-slate-400 hover:text-white rounded hover:bg-slate-800"
                  title="Copy"
                >
                  {copiedText ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>

            {selectedNode.metadata?.caseNumber && (
              <div className="flex items-center justify-between py-1 border-t border-slate-800/80">
                <span className="text-slate-400">Case Reference</span>
                <span className="font-mono text-slate-200 font-medium">
                  {selectedNode.metadata.caseNumber}
                </span>
              </div>
            )}

            {selectedNode.metadata?.campaignCode && (
              <div className="flex items-center justify-between py-1 border-t border-slate-800/80">
                <span className="text-slate-400">Campaign Code</span>
                <span className="font-mono text-amber-400 font-medium">
                  {selectedNode.metadata.campaignCode}
                </span>
              </div>
            )}

            {selectedNode.metadata?.country && (
              <div className="flex items-center justify-between py-1 border-t border-slate-800/80">
                <span className="text-slate-400">GeoIP Country</span>
                <span className="text-slate-200">{selectedNode.metadata.country}</span>
              </div>
            )}

            {selectedNode.metadata?.asn && (
              <div className="flex items-center justify-between py-1 border-t border-slate-800/80">
                <span className="text-slate-400">Network ASN</span>
                <span className="font-mono text-slate-200">AS{selectedNode.metadata.asn}</span>
              </div>
            )}

            {selectedNode.metadata?.filename && (
              <div className="flex items-center justify-between py-1 border-t border-slate-800/80">
                <span className="text-slate-400">Filename</span>
                <span className="font-mono text-slate-200 truncate max-w-[150px]">
                  {selectedNode.metadata.filename}
                </span>
              </div>
            )}
          </div>

          <div className="pt-2 border-t border-slate-800 flex items-center gap-2">
            {selectedNode.type === 'EMAIL' && selectedNode.metadata?.caseId && (
              <button
                id="graph-investigate-btn"
                onClick={() => {
                  const emailId = selectedNode.id.replace('email-', '');
                  navigate(`/cases/${selectedNode.metadata.caseId}/evidence/${emailId}`);
                }}
                className="w-full flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white font-medium text-xs transition-colors shadow-md shadow-cyan-600/20"
              >
                <span>Investigate Evidence</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            )}

            {selectedNode.type === 'CAMPAIGN' && (
              <button
                id="graph-view-campaign-btn"
                onClick={() => {
                  const campId = selectedNode.id.replace('campaign-', '');
                  navigate(`/campaigns/${campId}`);
                }}
                className="w-full flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-medium text-xs transition-colors shadow-md shadow-amber-600/20"
              >
                <span>View Full Campaign</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            )}

            {selectedNode.metadata?.caseId && selectedNode.type !== 'EMAIL' && (
              <button
                onClick={() => navigate(`/cases/${selectedNode.metadata.caseId}`)}
                className="w-full flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-medium text-xs transition-colors"
              >
                <span>Open Case</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
