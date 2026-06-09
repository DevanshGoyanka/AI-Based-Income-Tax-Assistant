export default function ClientsPage() {
  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Clients</h1>
        <button className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
          + Add Client
        </button>
      </div>
      <div className="bg-white rounded-lg shadow">
        <div className="p-4 border-b">
          <input
            type="text"
            placeholder="Search by PAN, name, or mobile..."
            className="w-full border rounded px-3 py-2"
          />
        </div>
        <div className="p-6 text-center text-gray-500">
          No clients found. Add your first client to get started.
        </div>
      </div>
    </div>
  );
}
