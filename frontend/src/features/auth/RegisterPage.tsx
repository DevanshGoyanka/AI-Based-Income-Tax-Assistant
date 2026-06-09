export default function RegisterPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h1 className="text-2xl font-bold mb-6 text-center">Register — TaxERP</h1>
        <p className="text-gray-600 mb-6 text-center">
          Create your CA firm account. Registration requires verification.
        </p>
        <form>
          <div className="mb-4">
            <label className="block text-sm font-medium mb-1">Firm Name</label>
            <input type="text" className="w-full border rounded px-3 py-2" required />
          </div>
          <div className="mb-4">
            <label className="block text-sm font-medium mb-1">Email</label>
            <input type="email" className="w-full border rounded px-3 py-2" required />
          </div>
          <div className="mb-4">
            <label className="block text-sm font-medium mb-1">Mobile</label>
            <input type="tel" className="w-full border rounded px-3 py-2" required />
          </div>
          <div className="mb-4">
            <label className="block text-sm font-medium mb-1">Password</label>
            <input type="password" className="w-full border rounded px-3 py-2" required />
          </div>
          <button type="submit" className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700">
            Register
          </button>
        </form>
      </div>
    </div>
  );
}
