export default function ITRComputationPage() {
  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Tax Computation</h1>
      <div className="bg-white rounded-lg shadow mb-6">
        <div className="border-b px-6 py-3 flex space-x-4">
          <button className="px-4 py-2 bg-blue-50 text-blue-700 rounded-t font-medium">Income Entry</button>
          <button className="px-4 py-2 text-gray-500 hover:text-gray-700">Deductions</button>
          <button className="px-4 py-2 text-gray-500 hover:text-gray-700">Tax Regime Comparison</button>
          <button className="px-4 py-2 text-gray-500 hover:text-gray-700">Computation Sheet</button>
        </div>
        <div className="p-6">
          <p className="text-gray-500">Select an income head to begin entering data.</p>
        </div>
      </div>
    </div>
  );
}
