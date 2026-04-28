import type { ReconciliationReport } from '../types/import.types';

interface ReconciliationModalProps {
  show: boolean;
  report: ReconciliationReport | null;
  onClose: () => void;
  onResolve: (item: any, action: 'USE_26AS' | 'USE_AIS' | 'MANUAL') => void;
}

export default function ReconciliationModal({ show, report, onClose, onResolve }: ReconciliationModalProps) {
  if (!show || !report) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-6xl w-full max-h-[90vh] overflow-hidden">
        <div className="p-6 border-b">
          <h2 className="text-2xl font-bold">Reconciliation Required</h2>
          <p className="text-gray-600 mt-2">
            ⚠️ We found discrepancies between your 26AS and AIS. Per Income Tax Department guidelines, 
            26AS (TRACES) values should be used for TDS credit claims.
          </p>
        </div>

        <div className="p-6 overflow-y-auto max-h-[60vh]">
          <table className="w-full border-collapse">
            <thead>
              <tr className="bg-gray-100">
                <th className="border p-2 text-left">Deductor</th>
                <th className="border p-2 text-left">TAN</th>
                <th className="border p-2 text-right">Income (26AS)</th>
                <th className="border p-2 text-right">Income (AIS)</th>
                <th className="border p-2 text-right">TDS (26AS)</th>
                <th className="border p-2 text-right">TDS (AIS)</th>
                <th className="border p-2 text-center">Action</th>
              </tr>
            </thead>
            <tbody>
              {report.items.map((item, idx) => (
                <tr key={idx} className="hover:bg-gray-50">
                  <td className="border p-2">{item.deductorName}</td>
                  <td className="border p-2 font-mono text-sm">{item.tan}</td>
                  <td className="border p-2 text-right">₹{item.income26AS.toLocaleString()}</td>
                  <td className="border p-2 text-right">₹{item.incomeAIS.toLocaleString()}</td>
                  <td className="border p-2 text-right font-semibold text-green-600">
                    ₹{item.tds26AS.toLocaleString()}
                  </td>
                  <td className="border p-2 text-right text-gray-500">₹{item.tdsAIS.toLocaleString()}</td>
                  <td className="border p-2 text-center">
                    <div className="flex gap-2 justify-center">
                      <button
                        onClick={() => onResolve(item, 'USE_26AS')}
                        className="px-3 py-1 bg-green-600 text-white rounded text-sm hover:bg-green-700"
                      >
                        Use 26AS
                      </button>
                      <button
                        onClick={() => onResolve(item, 'USE_AIS')}
                        className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
                      >
                        Use AIS
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="p-6 border-t bg-gray-50 flex justify-between">
          <div className="text-sm text-gray-600">
            <strong>Recommendation:</strong> Use 26AS values for TDS credit (green column)
          </div>
          <button
            onClick={onClose}
            className="px-6 py-2 bg-gray-600 text-white rounded hover:bg-gray-700"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
