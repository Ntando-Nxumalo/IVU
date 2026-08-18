function success(res, data, status = 200) {
  return res.status(status).json({ success: true, data, error: null });
}

function failure(res, error, status = 400) {
  return res.status(status).json({ success: false, data: null, error });
}

module.exports = { success, failure };
