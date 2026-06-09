export default function NoticesPage() {
  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Notices</h1>
        <button className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
          Fetch New Notices
        </button>
      </div>
      <div className="bg-white rounded-lg shadow">
        <div className="p-4 border-b flex space-x-4">
          <select className="border rounded px-3 py-1 text-sm">
            <option>All Status</option>
            <option>New</option>
            <option>Reviewed</option>
            <option>Replied</option>
            <option>Resolved</option>
          </select>
          <select className="border rounded px-3 py-1 text-sm">
            <option>All Risk Levels</option>
            <option>High</option>
            <option>Medium</option>
            <option>Low</option>
          </select>
        </div>
        <div className="p-6 text-center text-gray-500">
          No notices found. Click "Fetch New Notices" to scrape from the ITD portal.
        </div>
      </div>
    </div>
  );
}
