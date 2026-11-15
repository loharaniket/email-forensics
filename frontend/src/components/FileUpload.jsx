import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, FileCheck, AlertCircle, CheckCircle2, Copy, ArrowRight, ShieldCheck, Cpu } from 'lucide-react';
import { uploadEmail } from '../api/emailApi';

export default function FileUpload({ onUploadSuccess }) {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [copied, setCopied] = useState(false);

  const [enableThreatIntel, setEnableThreatIntel] = useState(true);
  const [enableAiConsent, setEnableAiConsent] = useState(false);

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const validateAndSetFile = (file) => {
    setError(null);
    setResult(null);

    if (!file) return;

    if (!file.name.toLowerCase().endsWith('.eml')) {
      setError('Invalid file format. Please upload a valid RFC 5322 (.eml) email file.');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setError('File size exceeds the 10 MB limit.');
      return;
    }

    setSelectedFile(file);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileSelect = (e) => {
    if (e.target.files && e.target.files[0]) {
      validateAndSetFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    setIsUploading(true);
    setError(null);

    try {
      const response = await uploadEmail(selectedFile);
      setResult(response);
      if (onUploadSuccess) {
        onUploadSuccess(response);
      }
    } catch (err) {
      setError(err.message || 'Failed to upload email evidence.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleCopyHash = () => {
    if (!result?.sha256) return;
    navigator.clipboard.writeText(result.sha256);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleReset = () => {
    setSelectedFile(null);
    setResult(null);
    setError(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  return (
    <div className="bg-[#1e293b] border border-[#334155] rounded-xl p-6 shadow-xl text-slate-100">
      <div className="flex items-center justify-between mb-4 pb-3 border-b border-[#334155]">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Upload className="w-5 h-5 text-blue-400" />
            Upload Email Evidence
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Preserve original RFC 5322 .eml evidence with cryptographic SHA-256 integrity
          </p>
        </div>
        <span className="text-xs font-mono px-2.5 py-1 rounded bg-[#0f172a] text-blue-300 border border-[#334155]">
          Max: 10 MB
        </span>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-950/50 border border-red-500/40 rounded-lg flex items-start gap-2.5 text-xs text-red-200">
          <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
          <div className="flex-1">{error}</div>
        </div>
      )}

      {result ? (
        <div className="space-y-4">
          <div className="p-4 bg-[#0f172a] border border-emerald-500/40 rounded-lg space-y-3">
            <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
              <CheckCircle2 className="w-5 h-5" />
              <span>Evidence Preserved Successfully</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
              <div>
                <span className="text-slate-400 block text-[11px]">Evidence ID</span>
                <span className="font-mono text-white text-sm">#{result.id}</span>
              </div>
              <div>
                <span className="text-slate-400 block text-[11px]">Original File</span>
                <span className="font-mono text-slate-200 truncate block">{result.fileName}</span>
              </div>
            </div>

            <div>
              <span className="text-slate-400 block text-[11px] mb-1">SHA-256 Digest (Cryptographic Fingerprint)</span>
              <div className="flex items-center gap-2 p-2 bg-[#1e293b] rounded border border-[#334155]">
                <code className="text-[11px] text-blue-300 font-mono break-all flex-1">
                  {result.sha256}
                </code>
                <button
                  onClick={handleCopyHash}
                  className="px-2 py-1 text-xs bg-[#334155] hover:bg-[#475569] text-white rounded flex items-center gap-1 transition"
                  title="Copy SHA-256"
                >
                  <Copy className="w-3.5 h-3.5" />
                  <span>{copied ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
            </div>

            <div className="flex items-center gap-2 text-xs pt-1">
              <span className="text-slate-400">Status:</span>
              <span className="px-2 py-0.5 rounded bg-blue-900/50 text-blue-300 border border-blue-600/30 font-semibold">
                {result.status}
              </span>
              <span className="text-slate-500">•</span>
              <span className="text-slate-400">Stage:</span>
              <span className="text-slate-300 font-mono">{result.stage}</span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate(`/evidence/${result.id}`)}
              className="flex-1 py-2.5 px-4 bg-blue-600 hover:bg-blue-500 text-white font-medium text-sm rounded-lg flex items-center justify-center gap-2 transition shadow-md"
            >
              <span>View Investigation</span>
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              onClick={handleReset}
              className="py-2.5 px-4 bg-[#334155] hover:bg-[#475569] text-slate-200 text-sm rounded-lg transition"
            >
              Upload Another
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <div
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition ${
              dragActive
                ? 'border-blue-400 bg-blue-950/20'
                : 'border-[#334155] hover:border-slate-500 bg-[#0f172a]/50'
            }`}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".eml"
              onChange={handleFileSelect}
              className="hidden"
            />

            <div className="flex flex-col items-center justify-center gap-2">
              <div className="p-3 rounded-full bg-[#1e293b] border border-[#334155]">
                <FileCheck className="w-7 h-7 text-blue-400" />
              </div>
              <div className="text-sm font-medium text-slate-200">
                {selectedFile ? (
                  <span className="text-blue-400 font-mono">{selectedFile.name}</span>
                ) : (
                  <span>Drag &amp; drop your .eml file here, or click to browse</span>
                )}
              </div>
              <div className="text-xs text-slate-400">
                {selectedFile
                  ? `Size: ${formatFileSize(selectedFile.size)}`
                  : 'Supports RFC 5322 raw email messages'}
              </div>
            </div>
          </div>

          <div className="p-3 bg-[#0f172a] rounded-lg border border-[#334155] space-y-2 text-xs">
            <span className="font-semibold text-slate-300 block">Analysis Options</span>
            <label className="flex items-center gap-2 cursor-pointer text-slate-300">
              <input
                type="checkbox"
                checked={enableThreatIntel}
                onChange={(e) => setEnableThreatIntel(e.target.checked)}
                className="rounded border-[#334155] text-blue-600 focus:ring-0 bg-[#1e293b]"
              />
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                Query external threat reputation feeds (Safe Browsing, PhishTank, AbuseIPDB)
              </span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer text-slate-300">
              <input
                type="checkbox"
                checked={enableAiConsent}
                onChange={(e) => setEnableAiConsent(e.target.checked)}
                className="rounded border-[#334155] text-blue-600 focus:ring-0 bg-[#1e293b]"
              />
              <span className="flex items-center gap-1.5">
                <Cpu className="w-3.5 h-3.5 text-purple-400" />
                Allow AI-assisted threat evaluation (requires explicit approval of redacted preview)
              </span>
            </label>
          </div>

          <button
            onClick={handleUpload}
            disabled={!selectedFile || isUploading}
            className={`w-full py-2.5 px-4 rounded-lg font-medium text-sm flex items-center justify-center gap-2 transition shadow-md ${
              !selectedFile || isUploading
                ? 'bg-slate-700 text-slate-400 cursor-not-allowed'
                : 'bg-blue-600 hover:bg-blue-500 text-white cursor-pointer'
            }`}
          >
            {isUploading ? (
              <>
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                <span>Preserving Evidence &amp; Hashing...</span>
              </>
            ) : (
              <>
                <FileCheck className="w-4 h-4" />
                <span>Upload &amp; Preserve Evidence</span>
              </>
            )}
          </button>
        </div>
      )}
    </div>
  );
}
