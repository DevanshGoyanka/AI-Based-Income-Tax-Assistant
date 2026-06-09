export default function FilingPage() {
  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">ITR Filing</h1>
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center mb-8">
          {['Prefill', 'Income', 'Computation', 'Validate', 'Submit', 'Verify'].map((step, idx) => (
            <div key={step} className="flex items-center">
              <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-sm font-medium">
                {idx + 1}
              </div>
              <span className="ml-2 text-sm text-gray-600">{step}</span>
              {idx < 5 && <div className="w-12 h-0.5 bg-gray-200 mx-2" />}
            </div>
          ))}
        </div>
        <p className="text-gray-500 text-center">Select a client to start the filing workflow.</p>
      </div>
    </div>
  );
}
